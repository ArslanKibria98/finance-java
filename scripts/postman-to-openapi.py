#!/usr/bin/env python3
"""
Converts KSA Islamic Financing Platform Postman collection to a single OpenAPI 3.0 spec.
All APIs go through Kong API Gateway. Tags mirror Postman folders.
"""

import json
import re
import sys
import yaml
from collections import OrderedDict

KONG_URL = "http://46.62.226.94:8000"
POSTMAN_FILE = "docs/postman/KSA-Islamic-Financing-Platform.postman_collection.json"
OUTPUT_FILE = "infrastructure/swagger/openapi.yml"


def wrap_success_response(inner_schema=None, description="Successful operation"):
    """Wrap a response schema in the standard ApiResponse envelope from foundational-infra-sdk."""
    envelope = {
        "description": description,
        "content": {
            "application/json": {
                "schema": {
                    "$ref": "#/components/schemas/ApiResponse"
                }
            }
        }
    }
    if inner_schema:
        envelope["content"]["application/json"]["schema"] = {
            "allOf": [
                {"$ref": "#/components/schemas/ApiResponse"},
                {
                    "type": "object",
                    "properties": {
                        "data": inner_schema
                    }
                }
            ]
        }
    return envelope


def error_ref(status_code, description):
    """Return a standard ErrorResponse reference for error status codes."""
    return {
        "description": description,
        "content": {
            "application/json": {
                "schema": {
                    "$ref": "#/components/schemas/ErrorResponse"
                }
            }
        }
    }


def parse_url(raw_url):
    """Convert Postman URL to OpenAPI path."""
    url = raw_url.replace("{{kongUrl}}", "")
    # Replace {{variable}} with {variable}
    url = re.sub(r'\{\{(\w+)\}\}', r'{\1}', url)
    # Remove query params for path (they go in parameters)
    if '?' in url:
        url = url.split('?')[0]
    return url


def extract_query_params(raw_url):
    """Extract query parameters from URL."""
    params = []
    if '?' in raw_url:
        query = raw_url.split('?')[1]
        for pair in query.split('&'):
            parts = pair.split('=')
            name = parts[0]
            example = parts[1] if len(parts) > 1 else ""
            params.append({
                "name": name,
                "in": "query",
                "required": False,
                "schema": {"type": "string"},
                "example": example
            })
    return params


def extract_path_params(path):
    """Extract path parameters like {customerId}."""
    params = []
    for match in re.finditer(r'\{(\w+)\}', path):
        params.append({
            "name": match.group(1),
            "in": "path",
            "required": True,
            "schema": {"type": "string"}
        })
    return params


def parse_body(raw_body):
    """Parse raw JSON body string into schema."""
    if not raw_body:
        return None
    try:
        data = json.loads(raw_body)
        properties = {}
        required = []
        for key, value in data.items():
            if isinstance(value, bool):
                properties[key] = {"type": "boolean", "example": value}
            elif isinstance(value, int):
                properties[key] = {"type": "integer", "example": value}
            elif isinstance(value, float):
                properties[key] = {"type": "number", "example": value}
            elif isinstance(value, list):
                properties[key] = {"type": "array", "items": {"type": "string"}, "example": value}
            elif isinstance(value, dict):
                properties[key] = {"type": "object", "example": value}
            else:
                properties[key] = {"type": "string", "example": str(value)}
            if value not in (None, "", [], {}):
                required.append(key)
        schema = {"type": "object", "properties": properties}
        if required:
            schema["required"] = required
        return schema
    except (json.JSONDecodeError, AttributeError):
        return None


def needs_auth(request):
    """Check if request needs auth (default is bearer from collection)."""
    auth = request.get("auth", {})
    if isinstance(auth, dict) and auth.get("type") == "noauth":
        return False
    return True


def process_items(items, tag_name, paths, tags_set):
    """Recursively process Postman items."""
    for item in items:
        if "item" in item:
            # It's a folder - use as sub-tag
            sub_tag = item.get("name", tag_name)
            # Clean up tag name
            sub_tag = re.sub(r'^\d+\s*-\s*', '', sub_tag).strip()
            if sub_tag not in tags_set:
                tags_set[sub_tag] = item.get("description", "")
            process_items(item["item"], sub_tag, paths, tags_set)
        elif "request" in item:
            # It's a request
            request = item["request"]
            method = request.get("method", "GET").lower()
            name = item.get("name", "")
            description = request.get("description", "")

            # Get URL
            url = request.get("url", "")
            if isinstance(url, dict):
                raw_url = url.get("raw", "")
            else:
                raw_url = url

            # Skip health check / non-Kong URLs
            if not raw_url.startswith("{{kongUrl}}") and not raw_url.startswith("{{keycloakUrl}}"):
                continue
            if "actuator/health" in raw_url or "kong" in raw_url.lower() and "8001" in raw_url:
                continue
            if raw_url.startswith("{{keycloakUrl}}"):
                continue

            path = parse_url(raw_url)
            if not path:
                continue

            # Build operation
            operation = {
                "summary": name,
                "tags": [tag_name],
                "operationId": re.sub(r'[^a-zA-Z0-9]', '_', name).strip('_'),
            }

            if description:
                operation["description"] = description

            # Parameters
            params = extract_path_params(path) + extract_query_params(raw_url)
            if params:
                operation["parameters"] = params

            # Request body
            body = request.get("body", {})
            if body and body.get("mode") == "raw" and body.get("raw"):
                schema = parse_body(body["raw"])
                if schema:
                    operation["requestBody"] = {
                        "required": True,
                        "content": {
                            "application/json": {
                                "schema": schema
                            }
                        }
                    }

            # Responses — wrapped in standard SDK envelope
            # Success: ApiResponse<T> {data, message, timestamp}
            # Error: ErrorResponse {timestamp, error_code, error, code, message, path, traceId, details}
            success_code = "201" if method == "post" and "create" in name.lower() else "200"
            operation["responses"] = {
                success_code: wrap_success_response(description="Successful operation"),
                "400": error_ref(400, "Bad request — validation failed"),
                "401": error_ref(401, "Unauthorized — invalid or missing JWT"),
                "404": error_ref(404, "Resource not found"),
                "422": error_ref(422, "Unprocessable entity — business rule violation"),
                "500": error_ref(500, "Internal server error")
            }
            if method == "delete":
                operation["responses"] = {
                    "204": {"description": "Successfully deleted (no content)"},
                    "401": error_ref(401, "Unauthorized — invalid or missing JWT"),
                    "404": error_ref(404, "Resource not found"),
                    "500": error_ref(500, "Internal server error")
                }

            # Security
            if needs_auth(request):
                operation["security"] = [{"BearerAuth": []}]
            else:
                operation["security"] = []

            # Add to paths
            if path not in paths:
                paths[path] = {}
            paths[path][method] = operation


def main():
    with open(POSTMAN_FILE, 'r') as f:
        collection = json.load(f)

    paths = OrderedDict()
    tags_set = OrderedDict()

    # Process all items
    for item in collection.get("item", []):
        if "item" in item:
            tag_name = item.get("name", "Default")
            tag_name = re.sub(r'^\d+\s*-\s*', '', tag_name).strip()
            desc = item.get("description", "")
            if tag_name not in tags_set:
                tags_set[tag_name] = desc
            process_items(item["item"], tag_name, paths, tags_set)
        elif "request" in item:
            process_items([item], "General", paths, tags_set)

    # Add login-with-pin (new API not in Postman yet)
    pin_path = "/identity-service/api/v1/auth/login-with-pin"
    paths[pin_path] = {
        "post": {
            "summary": "[identity-service] Customer Login with PIN",
            "description": "Authenticates a customer using their 10-digit Saudi National ID and 6-digit PIN set during onboarding. Returns JWT tokens.",
            "tags": ["Auth (identity-service via Kong)"],
            "operationId": "loginWithPin",
            "security": [],
            "requestBody": {
                "required": True,
                "content": {
                    "application/json": {
                        "schema": {
                            "type": "object",
                            "required": ["nationalId", "pin"],
                            "properties": {
                                "nationalId": {
                                    "type": "string",
                                    "description": "Saudi National ID (10 digits, starts with 1 or 2)",
                                    "example": "1234567890",
                                    "pattern": "^[12]\\d{9}$",
                                    "minLength": 10,
                                    "maxLength": 10
                                },
                                "pin": {
                                    "type": "string",
                                    "description": "6-digit app PIN set during onboarding",
                                    "example": "123456",
                                    "pattern": "^\\d{6}$",
                                    "minLength": 6,
                                    "maxLength": 6
                                }
                            }
                        }
                    }
                }
            },
            "responses": {
                "200": wrap_success_response(
                    inner_schema={
                        "type": "object",
                        "properties": {
                            "accessToken": {"type": "string", "description": "JWT access token"},
                            "refreshToken": {"type": "string", "description": "Refresh token"},
                            "expiresIn": {"type": "integer", "description": "Token expiry in seconds", "example": 300},
                            "tokenType": {"type": "string", "example": "Bearer"}
                        }
                    },
                    description="Authentication successful"
                ),
                "400": error_ref(400, "Invalid NID or PIN format"),
                "404": error_ref(404, "User not found for the given National ID"),
                "422": error_ref(422, "Invalid PIN or PIN not set")
            }
        }
    }

    # Build tags list
    tags = []
    for name, desc in tags_set.items():
        tag = {"name": name}
        if desc:
            # Truncate long descriptions
            tag["description"] = desc[:300] if len(desc) > 300 else desc
        tags.append(tag)

    # Build OpenAPI spec
    spec = OrderedDict()
    spec["openapi"] = "3.0.3"
    spec["info"] = {
        "title": "KSA Islamic Financing Platform API",
        "description": "Unified API documentation for the KSA Islamic Financing Platform.\n\nAll requests go through Kong API Gateway.\nURL pattern: /service-name/api/v1/...",
        "version": "1.0.0",
        "contact": {
            "name": "KSA Financing Platform Team"
        }
    }
    spec["servers"] = [
        {
            "url": KONG_URL,
            "description": "Kong API Gateway"
        }
    ]
    spec["tags"] = tags
    spec["paths"] = dict(paths)
    spec["components"] = {
        "securitySchemes": {
            "BearerAuth": {
                "type": "http",
                "scheme": "bearer",
                "bearerFormat": "JWT",
                "description": "JWT token from /identity-service/api/v1/auth/login or /identity-service/api/v1/auth/login-with-pin"
            }
        },
        "schemas": {
            "ApiResponse": {
                "type": "object",
                "description": "Standard success response envelope (from foundational-infra-sdk ApiResponse<T>)",
                "properties": {
                    "data": {
                        "description": "Response payload — type varies per endpoint"
                    },
                    "message": {
                        "type": "string",
                        "example": "OK",
                        "description": "Human-readable status message"
                    },
                    "timestamp": {
                        "type": "string",
                        "format": "date-time",
                        "example": "2026-02-26T07:36:23.118Z",
                        "description": "ISO-8601 response timestamp"
                    }
                },
                "required": ["data", "message", "timestamp"]
            },
            "ErrorResponse": {
                "type": "object",
                "description": "Standard error response envelope (from foundational-infra-sdk ErrorResponse)",
                "properties": {
                    "timestamp": {
                        "type": "string",
                        "format": "date-time",
                        "example": "2026-02-26T07:36:23Z",
                        "description": "ISO-8601 error timestamp"
                    },
                    "error_code": {
                        "type": "integer",
                        "example": 422,
                        "description": "HTTP status code"
                    },
                    "error": {
                        "type": "string",
                        "example": "Unprocessable Entity",
                        "description": "HTTP status reason phrase"
                    },
                    "code": {
                        "type": "string",
                        "example": "CUSTOMER.PROFILE.NOT_FOUND",
                        "description": "Domain error code (dot-separated)"
                    },
                    "message": {
                        "type": "string",
                        "example": "Customer profile not found",
                        "description": "Localized error message (Accept-Language: en/ar)"
                    },
                    "path": {
                        "type": "string",
                        "example": "/api/v1/customers/123",
                        "description": "Request path that caused the error"
                    },
                    "traceId": {
                        "type": "string",
                        "example": "a1b2c3d4",
                        "description": "Distributed tracing ID for debugging"
                    },
                    "details": {
                        "type": "object",
                        "additionalProperties": {"type": "string"},
                        "description": "Field-level validation errors (only for 400 responses)",
                        "example": {"nationalId": "must not be blank", "pin": "size must be between 6 and 6"}
                    }
                },
                "required": ["timestamp", "error_code", "error", "code", "message", "path"]
            }
        }
    }

    # Write YAML
    with open(OUTPUT_FILE, 'w') as f:
        yaml.dump(dict(spec), f, default_flow_style=False, allow_unicode=True, sort_keys=False, width=120)

    print(f"OpenAPI spec generated: {OUTPUT_FILE}")
    print(f"  Tags: {len(tags)}")
    print(f"  Paths: {len(paths)}")


if __name__ == "__main__":
    main()
