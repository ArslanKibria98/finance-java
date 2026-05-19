###########################################################################
# Init container — downloads Elastic APM Java agent once, copies into a
# shared volume that ALL Java services mount read-only at /apm-agent.
#
# Services attach via JAVA_TOOL_OPTIONS=-javaagent:/apm-agent/elastic-apm-agent.jar
# No Dockerfile change needed per service.
###########################################################################
FROM alpine:3.20

ARG APM_AGENT_VERSION=1.55.6

RUN apk add --no-cache curl ca-certificates && \
    mkdir -p /apm-agent && \
    curl -fsSL -o /apm-agent/elastic-apm-agent.jar \
      "https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/${APM_AGENT_VERSION}/elastic-apm-agent-${APM_AGENT_VERSION}.jar" && \
    chmod 0644 /apm-agent/elastic-apm-agent.jar

# On startup: copy jar to shared volume, then exit successfully.
# Compose dependent_on: condition: service_completed_successfully ensures
# Java services start only after agent is available.
CMD ["sh", "-c", "cp -u /apm-agent/elastic-apm-agent.jar /shared/elastic-apm-agent.jar && ls -lh /shared/ && echo 'APM agent ready in shared volume.'"]
