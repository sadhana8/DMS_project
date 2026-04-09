# Authentication API Documentation

## Overview
This document describes the authentication endpoints for the Document Management System.

## Base URL
```
http://localhost:8081/api/auth
```

## Endpoints

### 1. Register User
**POST** `/api/auth/register`

Register a new user account.

**Request Body:**
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "password123"
}
```

**Success Response (201):**
```json
{
  "message": "User registered successfully",
  "userId": 1,
  "username": "john_doe",
  "email": "john@example.com"
}
```

**Error Response (400):**
```json
"Email already exists"
```

### 2. Login
**POST** `/api/auth/login`

Authenticate a user and create a session.

**Request Body:**
```json
{
  "email": "john@example.com",
  "password": "password123"
}
```

**Success Response (200):**
```json
{
  "message": "Login successful",
  "userId": 1,
  "username": "john_doe",
  "email": "john@example.com"
}
```

**Error Response (401):**
```json
"Invalid email or password"
```

### 3. Get Current User
**GET** `/api/auth/me`

Get the currently authenticated user's information.

**Success Response (200):**
```json
{
  "message": "User details",
  "userId": 1,
  "username": "john_doe",
  "email": "john@example.com"
}
```

**Error Response (401):**
```json
"Not authenticated"
```

### 4. Logout
**POST** `/api/auth/logout`

Logout the current user and clear the session.

**Success Response (200):**
```json
"Logout successful"
```

## Web Pages

### Login Page
Access at: `http://localhost:8081/login.html`

### Signup Page
Access at: `http://localhost:8081/signup.html`

## Testing with cURL

### Register a new user:
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"testuser\",\"email\":\"test@example.com\",\"password\":\"password123\"}"
```

### Login:
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"test@example.com\",\"password\":\"password123\"}" \
  -c cookies.txt
```

### Get current user (using saved cookies):
```bash
curl -X GET http://localhost:8081/api/auth/me \
  -b cookies.txt
```

### Logout:
```bash
curl -X POST http://localhost:8081/api/auth/logout \
  -b cookies.txt
```

## Security Notes
- Passwords are encrypted using BCrypt
- Session-based authentication is used
- CSRF protection is disabled for API endpoints
- All endpoints except `/api/auth/register` and `/api/auth/login` require authentication
