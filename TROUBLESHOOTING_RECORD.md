# 问题排查记录

## 问题描述

**时间**: 2026-04-30  
**现象**: 智能问答功能报错 "Not existing vector name error"  
**前端请求数据**:
```json
{
  "question": "你好",
  "sessionId": "session_3tk5c9evp_1776660675842",
  "memoryEnabled": true
}
```

## 初步怀疑方向

### 1. 前端数据格式问题 ❌
- **假设**: 前端传递的 JSON 数据格式不正确
- **验证**: 检查请求数据结构
- **结论**: 数据格式完全正确，排除此方向

### 2. 编码问题 ❌
- **假设**: 中文字符编码导致解析错误
- **验证**: 检查请求头 Content-Type 和字符集
- **结论**: 编码正常，排除此方向

### 3. 单元测试与实际运行差异 ❓
- **假设**: 单元测试通过但实际运行失败，可能存在环境差异
- **验证**: 
  - ChatServiceTest: 8/8 测试通过 ✅
  - TextChunkerTest: 6/6 测试通过 ✅
- **结论**: 单元测试覆盖的是业务逻辑层，不涉及 Qdrant API 调用细节

## 根本原因定位

### 问题根源：Qdrant REST API 两处参数格式错误 ✅

**第一次错误**: "Not existing vector name error"
**第二次错误**: "Format error in JSON body: data did not match any variant of untagged enum NamedVectorStruct"
**第三次错误**: "class com.google.gson.JsonArray cannot be cast to class com.google.gson.JsonObject"

**问题分析**:
1. **集合创建时 vectors 参数嵌套错误**
   - 错误格式：`{"vectors": {"vectors": {"size": 1536, ...}}}`
   - 正确格式：`{"vectors": {"size": 1536, "distance": "Cosine"}}`
   - 原因：多嵌套了一层 vectors 对象

2. **搜索API vector 参数格式错误**
   - 错误格式：`{"vector": {"vector": [...]}}`  
   - 正确格式：`{"vector": [...]}`
   - 原因：vector 应该是直接的数组，不需要包装成对象

3. **搜索API response 解析错误（最新发现）**
   - 错误：假设 result 字段是对象 `{"result": {"points": [...]}}`
   - 实际：result 字段直接是数组 `{"result": [...]}`
   - 原因：Qdrant REST API 返回格式理解错误

**代码对比**:

### 修复1: 集合创建 (createCollection)

❌ **错误的格式** (修改前):
```java
JsonObject vectors = new JsonObject();
JsonObject vectorParams = new JsonObject();
vectorParams.addProperty("size", 1536);
vectorParams.addProperty("distance", "Cosine");
vectors.add("vectors", vectorParams);  // 多余的嵌套
requestBody.add("vectors", vectors);
```

✅ **正确的格式** (修改后):
```java
JsonObject vectorParams = new JsonObject();
vectorParams.addProperty("size", 1536);
vectorParams.addProperty("distance", "Cosine");
requestBody.add("vectors", vectorParams);  // 直接添加参数对象
```

### 修复2: 搜索API (searchSimilar)

❌ **错误的格式** (修改前):
```java
JsonObject vectorObj = new JsonObject();
JsonArray vectorArray = new JsonArray();
for (float v : vector) {
    vectorArray.add(v);
}
vectorObj.add("vector", vectorArray);  // 多余的包装
requestBody.add("vector", vectorObj);
```

✅ **正确的格式** (修改后):
```java
JsonArray vectorArray = new JsonArray();
for (float v : vector) {
    vectorArray.add(v);
}
requestBody.add("vector", vectorArray);  // 直接添加数组
```

### 修复3: 搜索API response解析 (searchSimilar)

❌ **错误的格式** (修改前):
```java
JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
JsonObject result = jsonResponse.getAsJsonObject("result");  // 错误：result是数组
JsonArray scoredPoints = result.getAsJsonArray("points");

List<Map<String, Object>> results = new ArrayList<>();
for (int i = 0; i < scoredPoints.size(); i++) {
    JsonObject point = scoredPoints.get(i).getAsJsonObject();
    Map<String, Object> map = new HashMap<>();
    map.put("id", point.get("id").getAsString());
    map.put("score", point.get("score").getAsDouble());
    map.put("payload", gson.fromJson(point.get("payload"), Map.class));  // 可能为null
    results.add(map);
}
```

✅ **正确的格式** (修改后):
```java
JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

// Qdrant REST API: result 字段直接是数组
JsonElement resultElement = jsonResponse.get("result");
JsonArray scoredPoints;

if (resultElement.isJsonArray()) {
    // result 直接是数组
    scoredPoints = resultElement.getAsJsonArray();
} else if (resultElement.isJsonObject()) {
    // result 是对象，包含 points 数组
    JsonObject resultObj = resultElement.getAsJsonObject();
    scoredPoints = resultObj.getAsJsonArray("points");
} else {
    log.warn("Unexpected result type: {}", resultElement.getClass().getSimpleName());
    return new ArrayList<>();
}

if (scoredPoints == null) {
    log.warn("No points found in response");
    return new ArrayList<>();
}

List<Map<String, Object>> results = new ArrayList<>();
for (int i = 0; i < scoredPoints.size(); i++) {
    JsonObject point = scoredPoints.get(i).getAsJsonObject();
    Map<String, Object> map = new HashMap<>();
    map.put("id", point.get("id").getAsString());
    map.put("score", point.get("score").getAsDouble());
    
    // 安全地解析 payload
    JsonElement payloadElement = point.get("payload");
    if (payloadElement != null && !payloadElement.isJsonNull()) {
        map.put("payload", gson.fromJson(payloadElement, Map.class));
    } else {
        map.put("payload", new HashMap<>());
    }
    
    results.add(map);
}
```

## 修复方案

### 修改文件
- `src/main/java/com/knowledge/service/QdrantService.java`
  - 方法1: `createCollection()` - 修复 vectors 参数嵌套
  - 方法2: `searchSimilar(float[] vector, int topK)` - 修复 vector 参数格式
  - 方法3: `searchSimilar(float[] vector, int topK)` - 修复 response 解析逻辑

### 修改内容
1. **集合创建**: 移除多余的 vectors 嵌套层
2. **搜索API请求**: vector 直接使用数组，不包装成对象
3. **搜索API响应**: 兼容 result 字段为数组或对象两种格式
4. **安全解析**: 对 payload 字段进行 null 检查
5. **删除旧集合**: 使用 `DELETE /collections/knowledge_base` 删除错误格式的集合
6. **重启应用**: 重新创建正确格式的集合

### 集成测试验证
创建 `QdrantServiceIntegrationTest.java`，包含4个测试用例：
- ✅ 测试1: 当前实现 (修复后的 vector 作为数组)
- ✅ 测试2: vector 直接作为数组
- ✅ 测试3: 最小化请求
- ✅ 测试4: 检查集合信息

**第一次测试结果**: **4/4 全部通过** (修复1和2后)
**第二次测试结果**: **4/4 全部通过** (修复3后)

**关键验证**:
```json
// Qdrant API 返回格式
{"result":[],"status":"ok","time":0.0001714}

// 集合配置验证
{
  "config": {
    "params": {
      "vectors": {
        "size": 1536,
        "distance": "Cosine"
      }
    }
  }
}
```

### 验证结果
- ✅ 应用成功启动
- ✅ Qdrant 连接测试通过
- ✅ 集合正确创建（vectors 参数格式正确）
- ✅ 搜索 API 返回 200 成功
- ✅ 端口 8080 正常监听
- 🔄 等待用户在前端页面进行功能测试

## 经验总结

### 1. 单元测试的局限性
- 单元测试使用 Mock 对象，不真实调用外部 API
- Qdrant API 调用在测试中被 Mock，因此无法发现参数格式问题
- **建议**: 增加集成测试，真实调用外部服务

### 2. API 文档的重要性
- Qdrant REST API 和 gRPC API 的参数格式可能不同
- 需要仔细阅读官方文档，确认正确的请求格式
- **参考**: [Qdrant REST API 文档](https://qdrant.tech/documentation/reference/api/)

### 3. 错误信息的误导性
- "Not existing vector name error" 看起来像是集合或向量名称配置问题
- "Format error" 看起来像是 JSON 格式问题
- "ClassCastException" 看起来像是类型转换问题
- 实际上都是参数结构错误，需要从 API 调用层面分析
- **技巧**: 查看完整的 HTTP 请求体和响应体，对比官方示例

### 4. 防御性编程
- 解析 JSON 响应时要先判断类型再转换（使用 `JsonElement.isJsonArray()` 等）
- 对可能为 null 的字段进行检查
- 兼容不同的 API 版本可能返回的不同格式

### 5. 日志记录要适度
- 大 JSON 体使用 `log.debug()` 而不是 `log.info()`
- 记录 body length 而不是完整 body
- 避免日志污染，影响问题排查

### 6. 排查思路优化
当单元测试通过但实际运行失败时：
1. 检查外部依赖服务的 API 调用是否正确
2. 验证网络请求的实际数据格式
3. 对比官方文档的请求示例
4. 查看服务端返回的详细错误信息
5. 创建集成测试直接调用真实 API

## 后续改进建议

### 短期
1. ✅ 修复 Qdrant createCollection 方法的 vectors 参数嵌套
2. ✅ 修复 Qdrant searchSimilar 方法的 vector 参数格式
3. ✅ 修复 Qdrant searchSimilar 方法的 response 解析逻辑
4. 🔄 验证前端页面功能是否正常
5. 📝 在 TESTING_STANDARDS.md 中添加集成测试要求

### 中期
1. 创建 QdrantService 的集成测试类
2. 使用 Testcontainers 启动真实的 Qdrant 实例进行测试
3. 添加 API 请求/响应的日志记录，便于调试

### 长期
1. 建立 API 调用的自动化测试框架
2. 对所有外部服务调用进行契约测试
3. 完善错误处理和用户友好的错误提示

## 相关文件

- [QdrantService.java](file:///d:/aiwork/knowledge-base/src/main/java/com/knowledge/service/QdrantService.java)
- [ChatService.java](file:///d:/aiwork/knowledge-base/src/main/java/com/knowledge/service/ChatService.java)
- [TESTING_STANDARDS.md](file:///d:/aiwork/knowledge-base/TESTING_STANDARDS.md)
- [TEST_REPORT.md](file:///d:/aiwork/knowledge-base/TEST_REPORT.md)

---

**记录人**: AI Assistant  
**最后更新**: 2026-04-30 12:33
