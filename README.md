# ToDo REST API

Spring Boot 3.x, Spring Data JPA, H2로 만든 회원 기능 없는 ToDo REST API입니다. 할 일의 생성, 목록 조회, 단건 조회, 수정(완료 상태 변경 포함), 삭제를 제공합니다.

## 실행 방법

- JDK 21
- 별도 DB 설치나 환경 변수는 필요하지 않습니다. 실행 중인 데이터는 H2 인메모리 DB에 저장되며 서버를 종료하면 초기화됩니다.

프로젝트 루트에서 아래 한 명령으로 서버를 실행합니다.

```bash
./gradlew bootRun
```

서버는 `http://localhost:8080`에서 시작합니다.

통합 테스트는 다음 명령으로 실행합니다.

```bash
./gradlew test
```

## API 명세

### 할 일 응답

모든 정상 조회·생성·수정 응답은 다음 형식입니다. `id`와 `createdAt`은 서버가 설정하고, 새 할 일의 `completed`는 항상 `false`입니다.

```json
{
  "id": 1,
  "title": "장보기",
  "completed": false,
  "createdAt": "2026-09-27T16:00:00"
}
```

제목은 공백을 제외한 1~200자여야 합니다. 수정 요청에서는 `title`, `completed` 중 하나 이상을 보냅니다.

| 메서드 | 주소 | 요청 본문 | 성공 상태 | 응답 본문 |
|---|---|---|---|---|
| POST | `/api/v1/todos` | `{"title":"장보기"}` | 201 Created | 생성된 할 일 |
| GET | `/api/v1/todos` | 없음 | 200 OK | 할 일 배열 |
| GET | `/api/v1/todos/{id}` | 없음 | 200 OK | 할 일 하나 |
| PATCH | `/api/v1/todos/{id}` | `{"title":"우유 사기","completed":true}` | 200 OK | 수정된 할 일 |
| DELETE | `/api/v1/todos/{id}` | 없음 | 204 No Content | 없음 |

`PATCH`에서는 아래처럼 완료 상태만 바꿀 수도 있습니다.

```json
{
  "completed": true
}
```

### 오류 응답

모든 오류는 상태 코드와 관계없이 같은 본문 모양으로 응답합니다.

```json
{
  "code": "VALIDATION_ERROR",
  "message": "제목은 공백이 아닌 1~200자여야 합니다."
}
```

| 상태 | 코드 | 발생 조건 |
|---|---|---|
| 400 Bad Request | `VALIDATION_ERROR` | 제목이 공백이거나 200자를 초과한 경우, 수정할 값이 없는 경우, 요청 형식이 잘못된 경우 |
| 404 Not Found | `TODO_NOT_FOUND` | 조회·수정·삭제 대상 ID가 없는 경우 |
| 500 Internal Server Error | `INTERNAL_SERVER_ERROR` | 처리하지 못한 서버 오류 |

## 설계 설명

- URI는 리소스 컬렉션을 뜻하는 복수형 `/todos`를 사용하고, 한 건은 `/todos/{id}`로 식별합니다.
- 생성은 새 리소스가 만들어졌음을 나타내는 `201`, 조회·수정은 결과를 돌려주므로 `200`, 응답 본문이 필요 없는 삭제는 `204`를 사용했습니다.
- JPA 엔티티는 Controller 밖으로 노출하지 않고, 요청은 `TodoCreateRequest`·`TodoUpdateRequest`, 응답은 `TodoResponse` DTO로 분리했습니다.
- 별도 준비 없이 한 명령으로 실행할 수 있도록 H2 인메모리 DB를 선택했습니다.
- Controller → Service → Repository로 역할을 분리했습니다. Service는 트랜잭션 안에서 엔티티를 변경·삭제하고, 없는 ID는 전용 예외로 404로 변환합니다.

## curl 실행 예시

아래의 시간 값은 실행 시점에 따라 달라집니다.

### 1. 생성

```bash
curl -i -X POST http://localhost:8080/api/v1/todos \
  -H 'Content-Type: application/json' \
  -d '{"title":"장보기"}'
```

```http
HTTP/1.1 201 Created
Content-Type: application/json

{"id":1,"title":"장보기","completed":false,"createdAt":"2026-09-27T16:00:00"}
```

### 2. 목록 조회

```bash
curl -i http://localhost:8080/api/v1/todos
```

```http
HTTP/1.1 200 OK
Content-Type: application/json

[{"id":1,"title":"장보기","completed":false,"createdAt":"2026-09-27T16:00:00"}]
```

### 3. 완료 처리

```bash
curl -i -X PATCH http://localhost:8080/api/v1/todos/1 \
  -H 'Content-Type: application/json' \
  -d '{"completed":true}'
```

```http
HTTP/1.1 200 OK
Content-Type: application/json

{"id":1,"title":"장보기","completed":true,"createdAt":"2026-09-27T16:00:00"}
```

### 4. 삭제

```bash
curl -i -X DELETE http://localhost:8080/api/v1/todos/1
```

```http
HTTP/1.1 204 No Content
```

### 5. 400 검증 오류

```bash
curl -i -X POST http://localhost:8080/api/v1/todos \
  -H 'Content-Type: application/json' \
  -d '{"title":"   "}'
```

```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{"code":"VALIDATION_ERROR","message":"제목은 공백이 아닌 1~200자여야 합니다."}
```

### 6. 404 없는 할 일

```bash
curl -i http://localhost:8080/api/v1/todos/9999
```

```http
HTTP/1.1 404 Not Found
Content-Type: application/json

{"code":"TODO_NOT_FOUND","message":"할 일을 찾을 수 없습니다. id=9999"}
```

## 테스트

`TodoControllerIntegrationTest`는 MockMvc와 H2를 사용해 다음을 검증합니다.

- 생성 → 목록 조회 → 완료 처리 → 삭제 → 삭제 후 404 정상 흐름
- 공백 제목과 200자 초과 제목의 400 응답
- 없는 ID의 조회·수정·삭제가 모두 404로 응답하는지
