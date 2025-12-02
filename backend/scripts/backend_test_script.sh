#!/bin/bash

# Base URL
BASE_URL="http://localhost:8080/api"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Helper function to print section headers
print_header() {
    echo -e "\n${GREEN}=== $1 ===${NC}"
}

# Helper function to check response code
check_response() {
    if [ "$1" -ge 200 ] && [ "$1" -lt 300 ]; then
        echo -e "${GREEN}Success ($1)${NC}"
    else
        echo -e "${RED}Failed ($1)${NC}"
        # exit 1 # Optional: exit on failure
    fi
}

# 1. Auth - Register
print_header "1. Register User"
REGISTER_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/register" \
    -H "Content-Type: application/json" \
    -d '{
        "username": "testuser_'$(date +%s)'",
        "name": "Test User",
        "password": "password123",
        "email": "test_'$(date +%s)'@example.com"
    }')
HTTP_CODE=$(echo "$REGISTER_RESPONSE" | tail -n1)
BODY=$(echo "$REGISTER_RESPONSE" | sed '$d')
echo "Response: $BODY"
check_response "$HTTP_CODE"

USERNAME=$(echo "$BODY" | jq -r '.username')
echo "Registered Username: $USERNAME"

# 2. Auth - Login
print_header "2. Login"
LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{
        "username": "'$USERNAME'",
        "password": "password123"
    }')
HTTP_CODE=$(echo "$LOGIN_RESPONSE" | tail -n1)
BODY=$(echo "$LOGIN_RESPONSE" | sed '$d')
echo "Response: $BODY"
check_response "$HTTP_CODE"

ACCESS_TOKEN=$(echo "$BODY" | jq -r '.accessToken')
REFRESH_TOKEN=$(echo "$BODY" | jq -r '.refreshToken')
echo "Access Token: ${ACCESS_TOKEN:0:20}..."

# 3. Auth - Refresh Token
print_header "3. Refresh Token"
REFRESH_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/refresh" \
    -H "Content-Type: application/json" \
    -d '{
        "refreshToken": "'$REFRESH_TOKEN'"
    }')
HTTP_CODE=$(echo "$REFRESH_RESPONSE" | tail -n1)
BODY=$(echo "$REFRESH_RESPONSE" | sed '$d')
echo "Response: $BODY"
check_response "$HTTP_CODE"

NEW_ACCESS_TOKEN=$(echo "$BODY" | jq -r '.accessToken')
if [ "$NEW_ACCESS_TOKEN" != "null" ]; then
    ACCESS_TOKEN=$NEW_ACCESS_TOKEN
    echo "Token Refreshed"
fi

# 4. User - Get Current User
print_header "4. Get Current User"
ME_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/users/me" \
    -H "Authorization: Bearer $ACCESS_TOKEN")
HTTP_CODE=$(echo "$ME_RESPONSE" | tail -n1)
BODY=$(echo "$ME_RESPONSE" | sed '$d')
echo "Response: $BODY"
check_response "$HTTP_CODE"

USER_ID=$(echo "$BODY" | jq -r '.id')
echo "User ID: $USER_ID"

# 5. Organization - Search
print_header "5. Search Organizations"
ORG_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/organizations?keyword=Test" \
    -H "Authorization: Bearer $ACCESS_TOKEN")
HTTP_CODE=$(echo "$ORG_RESPONSE" | tail -n1)
BODY=$(echo "$ORG_RESPONSE" | sed '$d')
echo "Response: $BODY"
check_response "$HTTP_CODE"

# 6. OpenApiSurvey - List
print_header "6. List OpenApi Surveys"
SURVEY_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/surveys?page=0&size=5" \
    -H "Authorization: Bearer $ACCESS_TOKEN")
HTTP_CODE=$(echo "$SURVEY_RESPONSE" | tail -n1)
BODY=$(echo "$SURVEY_RESPONSE" | sed '$d')
echo "Response: $BODY"
check_response "$HTTP_CODE"

SURVEY_ID=$(echo "$BODY" | jq -r '.content[0].id // empty')
if [ -z "$SURVEY_ID" ] || [ "$SURVEY_ID" == "null" ]; then
    echo "No surveys found, using dummy ID 1"
    SURVEY_ID=1
else
    echo "Found Survey ID: $SURVEY_ID"
fi

# 7. OpenApiSurvey - Get By ID
print_header "7. Get OpenApi Survey By ID"
SURVEY_DETAIL_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/surveys/$SURVEY_ID" \
    -H "Authorization: Bearer $ACCESS_TOKEN")
HTTP_CODE=$(echo "$SURVEY_DETAIL_RESPONSE" | tail -n1)
BODY=$(echo "$SURVEY_DETAIL_RESPONSE" | sed '$d')
echo "Response: $BODY"
check_response "$HTTP_CODE"

# 8. SR - Create
print_header "8. Create SR"
SR_CREATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/sr" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "title": "Test SR Title",
        "description": "Test SR Description",
        "priority": "MEDIUM",
        "applicantName": "Test Applicant",
        "applicantPhone": "010-1234-5678"
    }')
HTTP_CODE=$(echo "$SR_CREATE_RESPONSE" | tail -n1)
BODY=$(echo "$SR_CREATE_RESPONSE" | sed '$d')
echo "Response: $BODY"
check_response "$HTTP_CODE"

SR_ID=$(echo "$BODY" | jq -r '.id')
echo "Created SR ID: $SR_ID"

# 9. SR - List
print_header "9. List SRs"
SR_LIST_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/sr?page=0&size=5" \
    -H "Authorization: Bearer $ACCESS_TOKEN")
HTTP_CODE=$(echo "$SR_LIST_RESPONSE" | tail -n1)
BODY=$(echo "$SR_LIST_RESPONSE" | sed '$d')
echo "Response: $BODY"
check_response "$HTTP_CODE"

# 10. SR - Get By ID
print_header "10. Get SR By ID"
SR_DETAIL_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/sr/$SR_ID" \
    -H "Authorization: Bearer $ACCESS_TOKEN")
HTTP_CODE=$(echo "$SR_DETAIL_RESPONSE" | tail -n1)
BODY=$(echo "$SR_DETAIL_RESPONSE" | sed '$d')
echo "Response: $BODY"
check_response "$HTTP_CODE"

# 11. SR - Update
print_header "11. Update SR"
SR_UPDATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/sr/$SR_ID" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "title": "Updated SR Title",
        "description": "Updated SR Description",
        "priority": "HIGH",
        "status": "IN_PROGRESS"
    }')
HTTP_CODE=$(echo "$SR_UPDATE_RESPONSE" | tail -n1)
BODY=$(echo "$SR_UPDATE_RESPONSE" | sed '$d')
echo "Response: $BODY"
check_response "$HTTP_CODE"

# 12. SR - Update Status
print_header "12. Update SR Status"
SR_STATUS_RESPONSE=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/sr/$SR_ID/status" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "status": "RESOLVED"
    }')
HTTP_CODE=$(echo "$SR_STATUS_RESPONSE" | tail -n1)
BODY=$(echo "$SR_STATUS_RESPONSE" | sed '$d')
echo "Response: $BODY"
check_response "$HTTP_CODE"

# 13. SR - Create History
print_header "13. Create SR History"
HISTORY_CREATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/sr/$SR_ID/histories" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "content": "This is a test history comment."
    }')
HTTP_CODE=$(echo "$HISTORY_CREATE_RESPONSE" | tail -n1)
BODY=$(echo "$HISTORY_CREATE_RESPONSE" | sed '$d')
echo "Response: $BODY"
check_response "$HTTP_CODE"

# 14. SR - Get Histories
print_header "14. Get SR Histories"
HISTORY_LIST_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/sr/$SR_ID/histories" \
    -H "Authorization: Bearer $ACCESS_TOKEN")
HTTP_CODE=$(echo "$HISTORY_LIST_RESPONSE" | tail -n1)
BODY=$(echo "$HISTORY_LIST_RESPONSE" | sed '$d')
echo "Response: $BODY"
check_response "$HTTP_CODE"

# 15. SR - Delete
print_header "15. Delete SR"
SR_DELETE_RESPONSE=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL/sr/$SR_ID" \
    -H "Authorization: Bearer $ACCESS_TOKEN")
HTTP_CODE=$(echo "$SR_DELETE_RESPONSE" | tail -n1)
echo "Response Code: $HTTP_CODE"
check_response "$HTTP_CODE"

echo -e "\n${GREEN}All tests completed.${NC}"
