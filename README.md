# nva-courses-api
This repository contains components for backing an API to get course information from FS
(Felles Studentsystem).

The Courses API is used from a feature in DLR to give access to digital learning resources
(Digitale LæringsRessurser) to students that is currently attending one or more courses. Only a few
of DLR's customers have subscribed to this feature, and this is handled by configuration. Generally,
the API returns an empty list of courses if something fails in the FS integration for the institution
or if the institution is not configured to use it. 

## Courses currently taught by institution of logged-in user
Lambda for fetching courses for currently taught courses in the institution where the currently
logged-in user belongs. The institution of the currently logged-in user is assumed to be part of the
```RequestInfo``` object provided by the ```ApiGatewayHandler``` NVA framework class. More
specifically from the ```getTopLevelOrgCristinId()``` method. This is an organzation URI, so
currently we sub-string this URI to pick the institution code. This should be made more readily
available from the framework eventually.

### Required configuration
The following configuration must be made available in the Secrets Manager in the AWS account
where it will be deployed:
Key: ```fs-config```

Value:
```json
{
  "baseUri": "https://api.fellesstudentsystem.no",
  "institutions": [
    {
      "code": 215,
      "username": "***",
      "password": "***"
    }
  ]
}
```
