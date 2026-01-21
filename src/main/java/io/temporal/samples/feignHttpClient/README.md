# Feign HTTP Client Sample

This sample demonstrates how to use Feign HTTP client within Temporal activities to make external API calls.

## Features

- **Feign Integration**: Uses Feign declarative HTTP client for API calls
- **Debug Logging**: Configured to show Feign debug logs in console output
- **Error Handling**: Proper exception handling for HTTP failures
- **JSONPlaceholder API**: Makes calls to a public REST API for testing

## Running the Sample

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.feignHttpClient.Starter
```

## What it does

1. Creates a Feign client targeting JSONPlaceholder API
2. Executes a workflow that calls an activity
3. The activity uses Feign to fetch a post from the API
4. Returns the post data as a formatted string

## Logging

The sample includes debug-level logging for Feign HTTP requests and responses. You'll see detailed HTTP traffic in the console output including:

- Request URLs and headers
- Response status codes and bodies
- Timing information

e.g.

```17:09:48.236 [workflow-method-FeignHttpClientWorkflow-019bdf2c-6875-7785-ac3b-4725f1058a09] INFO  i.t.s.f.Starter$HttpWorkflowImpl - Starting workflow to process post ID: 1
17:09:48.275 [Activity Executor taskQueue="FeignHttpClientTaskQueue", namespace="default": 1] INFO  i.t.s.f.Starter$HttpActivitiesImpl - Fetching post with ID: 1
17:09:48.276 [Activity Executor taskQueue="FeignHttpClientTaskQueue", namespace="default": 1] DEBUG feign.Logger - [JsonPlaceholderClient#getPost] ---> GET https://jsonplaceholder.typicode.com/posts/ HTTP/1.1
17:09:48.276 [Activity Executor taskQueue="FeignHttpClientTaskQueue", namespace="default": 1] DEBUG feign.Logger - [JsonPlaceholderClient#getPost] Content-Length: 1
17:09:48.276 [Activity Executor taskQueue="FeignHttpClientTaskQueue", namespace="default": 1] DEBUG feign.Logger - [JsonPlaceholderClient#getPost] 
17:09:48.276 [Activity Executor taskQueue="FeignHttpClientTaskQueue", namespace="default": 1] DEBUG feign.Logger - [JsonPlaceholderClient#getPost] 1
17:09:48.276 [Activity Executor taskQueue="FeignHttpClientTaskQueue", namespace="default": 1] DEBUG feign.Logger - [JsonPlaceholderClient#getPost] ---> END HTTP (1-byte body)
17:09:49.100 [Activity Executor taskQueue="FeignHttpClientTaskQueue", namespace="default": 1] DEBUG feign.Logger - [JsonPlaceholderClient#getPost] <--- HTTP/1.1 201 Created (823ms)
17:09:49.100 [Activity Executor taskQueue="FeignHttpClientTaskQueue", namespace="default": 1] DEBUG feign.Logger - [JsonPlaceholderClient#getPost] access-control-allow-credentials: true
17:09:49.100 [Activity Executor taskQueue="FeignHttpClientTaskQueue", namespace="default": 1] DEBUG feign.Logger - [JsonPlaceholderClient#getPost] access-control-expose-headers: Location
17:09:49.100 [Activity Executor taskQueue="FeignHttpClientTaskQueue", namespace="default": 1] DEBUG feign.Logger - [JsonPlaceholderClient#getPost] alt-svc: h3=":443"; ma=86400
17:09:49.100 [Activity Executor taskQueue="FeignHttpClientTaskQueue", namespace="default": 1] DEBUG feign.Logger - [JsonPlaceholderClient#getPost] cache-control: no-cache
17:09:49.100 [Activity Executor taskQueue="FeignHttpClientTaskQueue", namespace="default": 1] DEBUG feign.Logger - [JsonPlaceholderClient#getPost] cf-cache-status: DYNAMIC
17:09:49.100 [Activity Executor taskQueue="FeignHttpClientTaskQueue", namespace="default": 1] DEBUG feign.Logger - [JsonPlaceholderClient#getPost] cf-ray: 9c14a5165fcc1a6e-MEL
17:09:49.100 [Activity Executor taskQueue="FeignHttpClientTaskQueue", namespace="default": 1] DEBUG feign.Logger - [JsonPlaceholderClient#getPost] connection: keep-alive
17:09:49.100 [Activity Executor taskQueue="FeignHttpClientTaskQueue", namespace="default": 1] DEBUG feign.Logger - [JsonPlaceholderClient#getPost] content-length: 26
17:09:49.100 [Activity Executor taskQueue="FeignHttpClientTaskQueue", namespace="default": 1] DEBUG feign.Logger - [JsonPlaceholderClient#getPost] content-type: application/json; charset=utf-8
17:09:49.100 [Activity Executor taskQueue="FeignHttpClientTaskQueue", namespace="default": 1] DEBUG feign.Logger - [JsonPlaceholderClient#getPost] date: Wed, 21 Jan 2026 06:09:49 GMT
17:09:49.100 [Activity Executor taskQueue="FeignHttpClientTaskQueue", namespace="default": 1] DEBUG feign.Logger - [JsonPlaceholderClient#getPost] etag: W/"1a-drmLJg4Jv0TWUV/vJifcUE6av4E"
17:09:49.100 [Activity Executor taskQueue="FeignHttpClientTa
```
