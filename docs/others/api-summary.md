# API Summary

| Method | Endpoint | Purpose |
|---|---|---|
| POST | /api/urls | Create a shortened URL |
| GET | /api/urls/{shortCode} | Redirect to the original destination |
| GET | /api/urls/{shortCode}/analytics | Retrieve analytics for a short URL |
| GET | /api/urls | List, filter, and sort URLs |
| GET | /api/urls/search | Search URLs by short code, original URL, or custom alias |
| GET | /api/urls/dashboard/summary | Return dashboard summary metrics |
| GET | /api/urls/dashboard/top | Return the most clicked URLs |
| GET | /api/urls/dashboard/recent | Return the most recently created URLs |
| GET | /api/v1/health | Return service health |

Verified against live controller mappings on 2026-07-16 — corrects a discrepancy between README.md and earlier Swagger capture.
