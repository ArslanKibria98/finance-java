TOKEN=$(cat token_new.txt)
curl -s -X POST "http://localhost:8000/lending-service/api/v1/loan-applications/62d36093-c494-4133-8a11-79fecad3b465/bank-account" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"bankCode":"RJHI"}' | python3 -m json.tool
