第一轮：需求分析文档

```
你是一位专业的程序员，现在请你根据需求，帮我设计方案、人工确认、按需分步骤完成开发、自主执行测试验证、找我验收。
## 需求

我现在想要构建出一个厨师agent，下面是我的文档

### 需求分析

方便各位厨师能更好的查看老乡鸡的菜的配方，可以问agent一键查出菜品的原材料和做法，和每日推荐菜品

### 方案设计

数据库 导入

从[github.com/Gar-b-age/CookLikeHOC](https://github.com/Gar-b-age/CookLikeHOC)此处获取到菜品，将这些菜品导入到数据库中，方便查阅

agent 开发

LLM api 选择 deepseek-v4-flash, 理由：便宜

通过ETL处理CookLikeHOC中的文档，将其存储到向量数据库中，构建rag，加入工具调用，mcp（如搜索等）。

构建一个自主规划的智能体(base-react-toolcall-manus，或者其它架构的)

扩展功能：

用户系统等等


## 任务
完善需求分析

## 注意事项
(1)文件保存在/docs/ai-docs/{需求分析}.md
(2)需要java开发
```

第二轮：任务2-数据库结构

```
你是一位专业的程序员，现在请你根据需求，帮我设计方案、人工确认、按需分步骤完成开发、自主执行测试验证、找我验收。
## 需求
上一轮对话已经明确需求分析，现在需要编写sql创建库表，然后编辑脚本一键导入到数据库中
这一轮技术选型:
| 层面           | 技术                   | 版本   | 选型理由                   |
| -------------- | ---------------------- | ------ | -------------------------- |
| **语言**       | Java                   | 21     | 团队技术栈、生态成熟       |
| **框架**       | Spring Boot            | 3.4.x  | 微服务标准框架             |
| **ORM**        | MyBatis-Plus           | 3.5.15 | 灵活 SQL，分页插件         |
| **数据库**     | MySQL                  | 8.0.12 | 存储菜品结构化数据         |
| **LLM**        | DeepSeek-V4-Flash      | —      | 便宜、中文能力强           |
| **LLM 框架**   | Spring AI              | 1.1.0  | Java 生态 LLM 集成框架     |
## 任务

编写sql创建库表，然后编辑一键导入到数据库中脚本

## 注意
（1）如果你要编写代码，请使用context7获取最新的文档
（2）文件保存路径: sql/
（3）脚本语言不限制
```

第三轮：任务3-Spring Boot 项目初始化。

```
你是一位专业的程序员，现在请你根据需求，帮我设计方案、人工确认、按需分步骤完成开发、自主执行测试验证、找我验收。
## 需求
上一轮对话中，根据sql文件提供的sql和脚本，我已经构建好数据库，接下来初始化Spring Boot项目
| 层面           | 技术                   | 版本   | 选型理由                   |
| -------------- | ---------------------- | ------ | -------------------------- |
| **语言**       | Java                   | 21     | 团队技术栈、生态成熟       |
| **框架**       | Spring Boot            | 3.4.x  | 微服务标准框架             |
| **ORM**        | MyBatis-Plus           | 3.5.15 | 灵活 SQL，分页插件         |
| **数据库**     | MySQL                  | 8.0.12 | 存储菜品结构化数据         |
| **LLM**        | DeepSeek-V4-Flash      | —      | 便宜、中文能力强           |
| **LLM 框架**   | Spring AI              | 1.1.0  | Java 生态 LLM 集成框架     |
| **tool库**     | hutool-all        | 5.8.38 | 便于开发                  |
| **openapi**    | kinf4j              | 4.4.0 | 调试接口                     |
## 任务

初始化Spring Boot项目
创建 Maven 项目，配置 pom.xml（Spring Boot 3.4.x + MyBatis-Plus 3.5.15 + Spring AI 1.1.0
application.yml 连接 MySQL，配置 MyBatis-Plus,kinf4j
 生成 Entity/Mapper
暂时不搭建 Docker 环境
## 注意
（1）如果你要编写代码，请一定要使用context7获取最新的文档
（2）项目已经使用脚手架搭建，但是需要修改，因为spring ai还未适配springboot3.5
```

第四轮：配置rag

这轮必须开启plan模式

至少要完成以下目标：

搜索搭建rag的最佳实现方式 

基于PGvector的方式，配置rag

基于内存的向量数据库的Bean

切割文档：

加载md文档，并将其切分，提取tags和meta

基于AI的文档元信息的增强器

基于token的切词器

cook-agent 上下文查询增强器

查询增强

上下文查询增强的工厂

RAG 检索增强顾问的工厂

查询重写器

```
你是一位专业的程序员，现在请你根据需求，帮我设计方案、人工确认、按需分步骤完成开发、自主执行测试验证、找我验收。
## 需求
这轮必须开启plan模式

至少要完成以下目标：

搜索搭建rag的最佳实现方式 

基于PGvector的方式，配置rag

基于内存的向量数据库的Bean

切割文档：

加载md文档，并将其切分，提取tags和meta

基于AI的文档元信息的增强器

基于token的切词器

cook-agent 上下文查询增强器

查询增强

上下文查询增强的工厂

RAG 检索增强顾问的工厂

查询重写器

## 任务

搜索搭建rag的最佳实现方式
配置rag

## 注意
（1）如果你要编写代码，请一定要使用context7获取最新的文档
（2）PGvector我现在还未安装，你帮我整理文档，保存在/docs/ai-docs/{PGVector配置}.md
（3）plan计划文档一定我找我却认，怎么选择技术栈
```

存在错误：

```
1./** RAG 专用 ChatClient（temperature=0，确保检索增强回答的准确性） */
    @Bean
    public ChatClient ragChatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder
                .defaultTemperature(0.0)
                .build();
    }
 不存在defaultTemperature()
 spring ai:
 AI 模型处理两类主要消息：用户消息（直接来自用户的输入）和系统消息（由系统生成以引导对话）。

这些消息通常包含占位符，运行时根据用户输入进行替换，从而定制 AI 模型对用户输入的响应。

还可以指定一些 Prompt 选项，如要使用的 AI 模型名称，以及控制生成输出随机性/创造性的 temperature 参数。
             // Derive a new OpenAiChatModel for Groq
            OpenAiChatModel groqModel = baseChatModel.mutate()
                .openAiApi(groqApi)
                .defaultOptions(OpenAiChatOptions.builder().model("llama3-70b-8192").temperature(0.5).build())
                .build();

            // Derive a new OpenAiChatModel for GPT-4
            OpenAiChatModel gpt4Model = baseChatModel.mutate()
                .openAiApi(gpt4Api)
                .defaultOptions(OpenAiChatOptions.builder().model("gpt-4").temperature(0.7).build())
                .build();
2.
    /** Token 文本切分器 — 中文标点语义切分 */
    @Bean
    public TokenTextSplitter tokenTextSplitter(RagProperties ragProperties) {
        RagProperties.TokenSplitter cfg = ragProperties.getTokenSplitter();
        return TokenTextSplitter.builder()
                .withChunkSize(cfg.getChunkSize())
                .withMinChunkSizeChars(cfg.getMinChunkSizeChars())
                .withMinChunkLengthToEmbed(cfg.getMinChunkLengthToEmbed())
                .withMaxNumChunks(cfg.getMaxNumChunks())
                .withKeepSeparator(cfg.isKeepSeparator())
                .withPunctuationMarks(List.of('。', '？', '！', '；', '\n'))
                .build();
    }
 
不存在.withPunctuationMarks(List.of('。', '？', '！', '；', '\n'))
spring ai:
TokenTextSplitter
TokenTextSplitter 是 TextSplitter 的实现类，采用 CL100K_BASE 编码按 Token 计数分割文本。

使用方法
@Component
class MyTokenTextSplitter {

    public List<Document> splitDocuments(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter();
        return splitter.apply(documents);
    }

    public List<Document> splitCustomized(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter(1000, 400, 10, 5000, true);
        return splitter.apply(documents);
    }
}
构造器选项
TokenTextSplitter 提供了两个构造函数选项：

TokenTextSplitter()：创建采用默认配置的分割器（Splitter）实例。

TokenTextSplitter(int defaultChunkSize, int minChunkSizeChars, int minChunkLengthToEmbed, int maxNumChunks, boolean keepSeparator)

参数
defaultChunkSize：每个文本块的目标 Token 数（默认值：800）。

minChunkSizeChars：每个文本块的最小字符数（默认值：350）。

minChunkLengthToEmbed：可嵌入分块的最小长度要求（默认值：5）。

maxNumChunks：单个文本生成的最大分块数限制（默认值：10000）。

keepSeparator： 是否在数据块中保留分隔符（如换行符）（默认值： true）。

行为
TokenTextSplitter 按以下流程处理文本内容：

使用 CL100K_BASE 编码将输入文本转换为 Token 序列。

根据 defaultChunkSize 将编码后的文本分割成块。

对于每个分块：

将分块后的 Token 序列解码还原为文本。

在达到 minChunkSizeChars 后，尝试寻找合适的分割点（句号、问号、感叹号或换行符）。

若找到分割点，则在该位置截断分块。

根据 keepSeparator 设置，对分块进行修剪并可选地移除换行符。

若结果分块长度大于 minChunkLengthToEmbed，则将其加入输出。

该过程持续运行，直至处理完所有 Token 或达到 maxNumChunks 限制。

剩余文本若长度超过 minChunkLengthToEmbed，则作为最终分块加入输出。

示例
Document doc1 = new Document("This is a long piece of text that needs to be split into smaller chunks for processing.",
        Map.of("source", "example.txt"));
Document doc2 = new Document("Another document with content that will be split based on token count.",
        Map.of("source", "example2.txt"));

TokenTextSplitter splitter = new TokenTextSplitter();
List<Document> splitDocuments = this.splitter.apply(List.of(this.doc1, this.doc2));

for (Document doc : splitDocuments) {
    System.out.println("Chunk: " + doc.getContent());
    System.out.println("Metadata: " + doc.getMetadata());
}
注意
TokenTextSplitter 采用 jtokkit 库的 CL100K_BASE 编码，该编码与新版 OpenAI 模型兼容。

splitter 会尽可能在句子边界处断开，以生成具有语义完整性的文本块。

原始文档的元数据将被保留，并复制到所有派生的文本块中。

若 copyContentFormatter 设为 true（默认行为），原始文档的内容格式化器也会复制到派生文本块。

该 splitter 特别适用于为大语言模型准备文本，通过确保每个分块都在模型的 Token 处理限制内，解决上下文长度约束问题。
3.        
MarkdownDocumentReaderConfig readerConfig = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(false)
                .withIncludeCodeBlock(false)
                .withIncludeBlockquote(false)
                .withAdditionalMetadata(fileMetadata)
                .build();
.withAdditionalMetadata(fileMetadata)
fileMetadara 提示需要提供Map(String, Object)
```

OK,接下需要测试RAG文档，生成一个简单的带rag的对话窗口test测试，需要

然后PGvector也在5432端口运行，通过docker实现

```
接下需要测试RAG文档，生成一个简单的带ragChatClinetest测试类

然后PGvector也在5432端口运行，通过docker实现

需要测试基于内存的rag和PGvector向量数据的测试用例

PGvector,需要导入的脚本

为了方便测试MyLoggerAdvisor
/**
 * 自定义日志 Advisor
 * 打印 info 级别日志、只输出单次用户提示词和 AI 回复的文本
 */
@Slf4j
public class MyLoggerAdvisor implements CallAdvisor, StreamAdvisor {

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public int getOrder() {
		return 0;
	}

	private ChatClientRequest before(ChatClientRequest request) {
		log.info("AI Request: {}", request.prompt());
		return request;
	}

	private void observeAfter(ChatClientResponse chatClientResponse) {
		log.info("AI Response: {}", chatClientResponse.chatResponse().getResult().getOutput().getText());
	}

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain chain) {
		chatClientRequest = before(chatClientRequest);
		ChatClientResponse chatClientResponse = chain.nextCall(chatClientRequest);
		observeAfter(chatClientResponse);
		return chatClientResponse;
	}

	@Override
	public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain chain) {
		chatClientRequest = before(chatClientRequest);
		Flux<ChatClientResponse> chatClientResponseFlux = chain.nextStream(chatClientRequest);
		return (new ChatClientMessageAggregator()).aggregateChatClientResponse(chatClientResponseFlux, this::observeAfter);
	}
}
```

生成错了应该是我提示词的问题

我发现这个要导入的文档很多，如果直接基于内存的方式导入的话会很慢，不如一次性的导入到PGvector中

所以需要一次性导入PGvector，下次启动程序可以直接通过PGvector检索。测试类也能快速的测试。

主要基于PGvector，基于内存的代码留着，常驻PGvector。然后不要动原有的代码



发现deepseek不支持embedding，只好换成国内的智谱的Embedding-3

[Embedding-3 - 智谱AI开放文档](https://docs.bigmodel.cn/cn/guide/models/embedding/embedding-3#java)

```
> ## Documentation Index
> Fetch the complete documentation index at: https://docs.bigmodel.cn/llms.txt
> Use this file to discover all available pages before exploring further.

# Embedding-3

## <div className="flex items-center"> <svg style={{maskImage: "url(https://mintcdn.com/zhipu-ef7018ed/6jZAOYw-eXEZh1pv/resource/icon/rectangle-list.svg?fit=max&auto=format&n=6jZAOYw-eXEZh1pv&q=85&s=018c661d2efce849f51ad05afdb0f876)", maskRepeat: "no-repeat", maskPosition: "center center",}} className={"h-6 w-6 bg-primary dark:bg-primary-light !m-0 shrink-0"} /> 概览 </div>

Embedding-3 是智谱AI 推出的第三代文本向量化模型，在前代基础上全面升级，提供更强的语义理解能力和更灵活的向量维度选择。该模型支持自定义向量维度，在保持高质量语义表示的同时，为不同应用场景提供了更优的性能和成本平衡。

<CardGroup cols={3}>
  <Card title="价格" icon={<svg style={{maskImage: "url(https://mintcdn.com/zhipu-ef7018ed/6jZAOYw-eXEZh1pv/resource/icon/coins.svg?fit=max&auto=format&n=6jZAOYw-eXEZh1pv&q=85&s=d140ba7189994790a79f83f5a763f59a)", maskRepeat: "no-repeat", maskPosition: "center center",}} className={"h-6 w-6 bg-primary dark:bg-primary-light !m-0 shrink-0"}/>}>
    0.5 元 / 百万 Tokens
  </Card>

  <Card title="输入模态" icon={<svg style={{maskImage: "url(https://mintcdn.com/zhipu-ef7018ed/6jZAOYw-eXEZh1pv/resource/icon/arrow-down-right.svg?fit=max&auto=format&n=6jZAOYw-eXEZh1pv&q=85&s=088a58fa0b1a4048d5c6fab7841133c8)", WebkitMaskImage: "url(https://mintcdn.com/zhipu-ef7018ed/6jZAOYw-eXEZh1pv/resource/icon/arrow-down-right.svg?fit=max&auto=format&n=6jZAOYw-eXEZh1pv&q=85&s=088a58fa0b1a4048d5c6fab7841133c8)", maskRepeat: "no-repeat", maskPosition: "center center",}} className={"h-6 w-6 bg-primary dark:bg-primary-light !m-0 shrink-0"} />}>
    文本
  </Card>

  <Card title="输出模态" icon={<svg style={{maskImage: "url(https://mintcdn.com/zhipu-ef7018ed/Skp28ct-clfAIOZo/resource/icon/arrow-down-left.svg?fit=max&auto=format&n=Skp28ct-clfAIOZo&q=85&s=1ed65b58aa7a484b387f01be25d99278)", WebkitMaskImage: "url(https://mintcdn.com/zhipu-ef7018ed/Skp28ct-clfAIOZo/resource/icon/arrow-down-left.svg?fit=max&auto=format&n=Skp28ct-clfAIOZo&q=85&s=1ed65b58aa7a484b387f01be25d99278)", maskRepeat: "no-repeat", maskPosition: "center center",}} className={"h-6 w-6 bg-primary dark:bg-primary-light !m-0 shrink-0"} />}>
    向量
  </Card>

  <Card title="上下文窗口" icon={<svg style={{maskImage: "url(https://mintcdn.com/zhipu-ef7018ed/Skp28ct-clfAIOZo/resource/icon/arrow-down-arrow-up.svg?fit=max&auto=format&n=Skp28ct-clfAIOZo&q=85&s=ccc051baa101b9a46d0d9bc5fad04877)", WebkitMaskImage: "url(https://mintcdn.com/zhipu-ef7018ed/Skp28ct-clfAIOZo/resource/icon/arrow-down-arrow-up.svg?fit=max&auto=format&n=Skp28ct-clfAIOZo&q=85&s=ccc051baa101b9a46d0d9bc5fad04877)", maskRepeat: "no-repeat", maskPosition: "center center",}} className={"h-6 w-6 bg-primary dark:bg-primary-light !m-0 shrink-0"} />}>
    8K
  </Card>

  <Card title="向量维度" icon={<svg style={{maskImage: "url(https://mintcdn.com/zhipu-ef7018ed/6jZAOYw-eXEZh1pv/resource/icon/maximize.svg?fit=max&auto=format&n=6jZAOYw-eXEZh1pv&q=85&s=743c202becf04d91d943f9014a3fe67f)", WebkitMaskImage: "url(https://mintcdn.com/zhipu-ef7018ed/6jZAOYw-eXEZh1pv/resource/icon/maximize.svg?fit=max&auto=format&n=6jZAOYw-eXEZh1pv&q=85&s=743c202becf04d91d943f9014a3fe67f)", maskRepeat: "no-repeat", maskPosition: "center center",}} className={"h-6 w-6 bg-primary dark:bg-primary-light !m-0 shrink-0"} />}>
    256-2048（可自定义）
  </Card>
</CardGroup>

## <div className="flex items-center"> <svg style={{maskImage: "url(https://mintcdn.com/zhipu-ef7018ed/6jZAOYw-eXEZh1pv/resource/icon/stars.svg?fit=max&auto=format&n=6jZAOYw-eXEZh1pv&q=85&s=eefc5fa680420566b18e2c3c1d30bb3d)", maskRepeat: "no-repeat", maskPosition: "center center",}} className={"h-6 w-6 bg-primary dark:bg-primary-light !m-0 shrink-0"} /> 推荐场景 </div>

<AccordionGroup>
  <Accordion title="高精度语义搜索" defaultOpen>
    利用更强的语义理解能力，实现更精准的文档检索和问答系统，特别适合专业领域的知识库构建。
  </Accordion>

  <Accordion title="智能推荐引擎" defaultOpen>
    基于用户行为和内容特征的深度理解，提供更个性化和精准的推荐服务，提升用户体验。
  </Accordion>

  <Accordion title="内容理解与分析" defaultOpen>
    深度分析文本内容的主题、情感和意图，用于舆情监控、内容审核和市场分析。
  </Accordion>

  <Accordion title="知识图谱构建" defaultOpen>
    通过语义向量化技术，自动发现实体关系，构建和完善知识图谱，支持复杂的知识推理。
  </Accordion>
</AccordionGroup>

## <div className="flex items-center"> <svg style={{maskImage: "url(https://mintcdn.com/zhipu-ef7018ed/6jZAOYw-eXEZh1pv/resource/icon/gauge-high.svg?fit=max&auto=format&n=6jZAOYw-eXEZh1pv&q=85&s=11e017cb0ce99d3d70ab7310e8728e18)", maskRepeat: "no-repeat", maskPosition: "center center",}} className={"h-6 w-6 bg-primary dark:bg-primary-light !m-0 shrink-0"} /> 使用资源 </div>

[体验中心](https://bigmodel.cn/trialcenter)：快速测试模型在业务场景上的效果<br />
[接口文档](/api-reference/%E6%A8%A1%E5%9E%8B-api/%E6%96%87%E6%9C%AC%E5%B5%8C%E5%85%A5)：API 调用方式

## <div className="flex items-center"> <svg style={{maskImage: "url(https://mintcdn.com/zhipu-ef7018ed/6jZAOYw-eXEZh1pv/resource/icon/arrow-up.svg?fit=max&auto=format&n=6jZAOYw-eXEZh1pv&q=85&s=2c1e1940f6d55086f84c6054cc093fac)", maskRepeat: "no-repeat", maskPosition: "center center",}} className={"h-6 w-6 bg-primary dark:bg-primary-light !m-0 shrink-0"} /> 详细介绍 </div>

<Steps>
  <Step title="模型升级" titleSize="h3">
    Embedding-3 在架构和训练数据上都进行了重大升级，显著提升了语义理解的准确性和泛化能力。新模型在多个评测基准上都取得了显著的性能提升。

    **核心升级：**

    * **增强语义理解**：更深层的语义捕捉能力，理解复杂的语言表达
    * **多语言优化**：针对中文、英文等多语言场景进行专门优化
    * **领域适应性**：在科技、金融、医疗等专业领域表现更佳
    * **鲁棒性提升**：对噪声文本和非标准表达有更强的容错能力
  </Step>

  <Step title="灵活维度选择" stepNumber={2} titleSize="h3">
    Embedding-3 支持自定义向量维度，用户可以根据具体应用场景选择最适合的维度，在性能和存储成本之间找到最佳平衡。

    **维度选项：**

    * **2048维（默认）**：最高精度，适合对准确性要求极高的场景
    * **1024维**：高精度与效率的平衡，适合大多数应用场景
    * **512维**：中等精度，适合大规模部署的场景
    * **256维**：较高效率，适合实时性要求高的场景

    **技术参数：**

    * 输入字符串数组中，单条请求最多支持 3072 个 Tokens，且数组最大不得超过 64 条
  </Step>
</Steps>

## <div className="flex items-center"> <svg style={{maskImage: "url(https://mintcdn.com/zhipu-ef7018ed/6jZAOYw-eXEZh1pv/resource/icon/code.svg?fit=max&auto=format&n=6jZAOYw-eXEZh1pv&q=85&s=2f67130d1597ee0b68135487ec31662f)", maskRepeat: "no-repeat", maskPosition: "center center",}} className={"h-6 w-6 bg-primary dark:bg-primary-light !m-0 shrink-0"} /> 调用示例 </div>

以下是一个完整的调用示例，帮助您快速上手 Embedding-3 模型。

<Tabs>
  <Tab title="cURL">
    ```bash theme={null}
    # 使用默认维度
    curl -X POST \
    https://open.bigmodel.cn/api/paas/v4/embeddings \
    -H "Authorization: Bearer your-api-key" \
    -H "Content-Type: application/json" \
    -d '{
        "model": "embedding-3",
        "input": "这是一段需要向量化的文本"
    }'

    # 自定义维度
    curl -X POST \
    https://open.bigmodel.cn/api/paas/v4/embeddings \
    -H "Authorization: Bearer your-api-key" \
    -H "Content-Type: application/json" \
    -d '{
        "model": "embedding-3",
        "input": "这是一段需要向量化的文本",
        "dimensions": 512
    }'
```
  </Tab>

  <Tab title="python">
    **安装 SDK**

    ```bash theme={null}
    # 安装最新版本
    pip install zai-sdk
    # 或指定版本
    pip install zai-sdk==0.2.2
    ```
    
    **验证安装**
    
    ```python theme={null}
    import zai
    print(zai.__version__)
    ```
    
    **调用示例**
    
    ```python theme={null}
    from zai import ZhipuAiClient
    
    client = ZhipuAiClient(api_key="your api key")
    response = client.embeddings.create(
        model="embedding-3", #填写需要调用的模型编码
        input=[
            "美食非常美味，服务员也很友好。",
            "这部电影既刺激又令人兴奋。",
            "阅读书籍是扩展知识的好方法。"
        ],
    )
    print(response)
    ```
  </Tab>

  <Tab title="Java">
    **安装 SDK**

    **Maven**
    
    ```xml theme={null}
    <dependency>
        <groupId>ai.z.openapi</groupId>
        <artifactId>zai-sdk</artifactId>
        <version>0.3.3</version>
    </dependency>
    ```
    
    **Gradle (Groovy)**
    
    ```groovy theme={null}
    implementation 'ai.z.openapi:zai-sdk:0.3.3'
    ```
    
    **调用示例**
    
    ```java theme={null}
    import ai.z.openapi.ZhipuAiClient;
    import ai.z.openapi.service.embedding.EmbeddingCreateParams;
    import ai.z.openapi.service.embedding.EmbeddingResponse;
    import java.util.Arrays;
    import java.util.List;
    
    public class Embedding3Example {
        public static void main(String[] args) {
            // 初始化客户端
            ZhipuAiClient client = ZhipuAiClient.builder().ofZHIPU()
                .apiKey("your-api-key")
                .build();
    
            // 创建向量化请求（自定义维度）
            EmbeddingCreateParams request = EmbeddingCreateParams.builder()
                .model("embedding-3")
                .input(Arrays.asList("Hello world", "How are you?", "How is the weather today?"))
                .dimensions(768)  // 指定768维
                .build();
    
            // 发送请求
            EmbeddingResponse response = client.embeddings().createEmbeddings(request);
            System.out.println("向量: " + response.getData());
        }
    }
    ```


执行到现在，embedding通过了，但是vector表中没有数据

cook_like_hoc=# SELECT * FROM vector_store LIMIT 5;
 id | content | metadata | embedding
----+---------+----------+-----------
(0 rows)

要做的是恢复之前完整的ETL的步骤，之前缺少了一步。

vector没有数据，查看原因

要将数据导入在PGvector中，常驻使用PGvector

生成测试类，测试rag的效果，不需要再controller层测试

```
你是一位专业的程序员，现在请你根据需求，帮我设计方案、人工确认、按需分步骤完成开发、自主执行测试验证、找我验收。
## 需求

执行到现在，embedding通过了，但是vector表中没有数据

cook_like_hoc=# SELECT * FROM vector_store LIMIT 5;
 id | content | metadata | embedding
----+---------+----------+-----------
(0 rows)

要做的是恢复之前完整的ETL的步骤，之前缺少了一步。

vector没有数据，查看原因

要将数据导入在PGvector中，常驻使用PGvector

生成测试类，测试rag的效果，不需要再controller层测试

## 任务

恢复之前完整的ETL步骤

vector没有数据，查看原因

要将数据导入在PGvector中，常驻使用PGvector

生成测试类，测试rag的效果，不需要再controller层测试


## 注意
（1）如果你要编写代码，请一定要使用context7获取最新的文档
```

发现问题，在测试教我做菜的时，有问题

```
    @Test
    @DisplayName("9. RAG Chat — 食材查询（高阈值 Advisor）")
    void testRagIngredientQuery() {
        String answer = ragChatClient.prompt()
                .user("家里有排骨，教我做一道菜")
                .advisors(advisorFactory.createIngredientMatchAdvisor())
                .call()
                .content();

        assertThat(answer).isNotBlank();
        log.info("问题: 家里有排骨，教我做一道菜");
        log.info("AI 回答: {}", answer);
    }
```

loginfo

```
2026-05-14T11:06:07.872+08:00  INFO 23016 --- [cook-agent] [           main] c.f.cookagent.rag.RagEffectivenessTest   : 问题: 家里有排骨，教我做一道菜
2026-05-14T11:06:07.872+08:00  INFO 23016 --- [cook-agent] [           main] c.f.cookagent.rag.RagEffectivenessTest   : AI 回答: I'm sorry, but I can't answer that question as it falls outside the scope of my knowledge base. If you have any other questions, I'd be happy to help!
```

排骨是存在的

```
cook_like_hoc=# SELECT
    metadata->>'recipe_name' as recipe,
    metadata->>'title' as section,
    LEFT(content, 80) as content
FROM vector_store
WHERE content LIKE '%排骨%'
LIMIT 5;
-[ RECORD 1 ]-------------------------------------------------------------------------------------------------------------------------------------------------------------
recipe  | 糖醋排骨
section | 配料
content | 排骨（猪肋排、猪小排、水、冰糖、酿造食醋、黄酒、酱油等）（老乡鸡中央厨房或供应商分切、滚揉、炸制、熬煮，详细成分、配比与步骤老乡鸡官方未公布，请依据个人口味适量
-[ RECORD 2 ]-------------------------------------------------------------------------------------------------------------------------------------------------------------
recipe  | 糖醋排骨
section | 步骤
content | 1300g 糖醋汁和 1550g 排骨倒入锅中，大火烧开；  烧开后炖煮 6 分钟；  倒入 30g 陈醋，继续炖煮 1 分钟至汤汁浓稠。
-[ RECORD 3 ]-------------------------------------------------------------------------------------------------------------------------------------------------------------
recipe  | 香芋蒸排骨
section | 配料
content | 香芋块（荔浦芋头） 调理排骨（猪肋排、大豆油、鸡蛋清、玉米淀粉、酱油、料酒、白胡椒等）（老乡鸡中央厨房制作，详细成分与配比官方未公布，请依据个人口味适量调整）
-[ RECORD 4 ]-------------------------------------------------------------------------------------------------------------------------------------------------------------
recipe  | 香芋蒸排骨
section | 步骤
content | 调味水配制（3 份）：将 300g 热水、13g 调味粉和 9g 熟猪油混合均匀；  取 100g 香芋块放入餐具，将 80g 调理排骨均匀平铺在香芋块上；
```

 将增强器去掉，换成默认的advisor

```
根据您提供的食材，可以尝试制作糖醋排骨。步骤如下：

1. 将1300克糖醋汁和1550克排骨倒入锅中，大火烧开。
2. 烧开后炖煮6分钟。
3. 倒入30克陈醋，继续炖煮1分钟至汤汁浓稠即可。

注意：具体调味可根据个人口味适量调整。
```

得出结论是增强器出现问题，是阈值存在问题：测试此阈值为`0.50 - 0.59`左右

```
    @Test
    @DisplayName("10. RAG Chat — 烹饪建议（低阈值 Advisor）")
    void testRagCookingAdvice() {
        String answer = ragChatClient.prompt()
                .user("炖菜有什么通用的技巧？")
                .advisors(advisorFactory.createCookingAdviceAdvisor())
                .call()
                .content();

        assertThat(answer).isNotBlank();
        log.info("问题: 炖菜有什么通用的技巧？");
        log.info("AI 回答: {}", answer);
    }
```

阈值怎么调整都是：

```
2026-05-14T11:20:32.117+08:00  INFO 15784 --- [cook-agent] [   ai-advisor-1] c.f.c.r.embedding.ZhipuAiEmbeddingModel  : ZhipuAI embedding: model=embedding-3, inputs=1, dim=1024
2026-05-14T11:20:32.661+08:00  INFO 15784 --- [cook-agent] [   ai-advisor-1] c.f.c.r.embedding.ZhipuAiEmbeddingModel  : ZhipuAI embedding done: usage={completion_tokens=0, prompt_tokens=11, total_tokens=11}
2026-05-14T11:20:33.582+08:00  INFO 15784 --- [cook-agent] [           main] c.f.cookagent.rag.RagEffectivenessTest   : 问题: 炖菜有什么通用的技巧？
2026-05-14T11:20:33.582+08:00  INFO 15784 --- [cook-agent] [           main] c.f.cookagent.rag.RagEffectivenessTest   : AI 回答: 根据上下文信息，无法总结出炖菜的通用技巧。
```

```
问题：调试过程中看不到调试信息？
希望添加rag的相似度，和检索出chunks的info
如：var documents = context.get(RetrievalAugmentationAdvisor.RETRIEVED_DOCUMENTS);
        log.info("Retrieved {} documents", documents.size());
        documents.forEach(doc -> 
            log.info("Similarity: {}, Content: {}", 
                doc.getMetadata().get("similarity"),
                doc.getContent().substring(0, Math.min(50, doc.getContent().length()))
            )
        );
需要这样的效果打印日志，这样便于调试
    @Test
    @DisplayName("10. RAG Chat — 烹饪建议（低阈值 Advisor）")
    void testRagCookingAdvice() {
        String answer = ragChatClient.prompt()
                .user("炖菜有什么通用的技巧？")
                .advisors(advisorFactory.createCookingAdviceAdvisor())
                .call()
                .content();

        assertThat(answer).isNotBlank();
        log.info("问题: 炖菜有什么通用的技巧？");
        log.info("AI 回答: {}", answer)
查出：
2026-05-14T11:20:32.117+08:00  INFO 15784 --- [cook-agent] [   ai-advisor-1] c.f.c.r.embedding.ZhipuAiEmbeddingModel  : ZhipuAI embedding: model=embedding-3, inputs=1, dim=1024
2026-05-14T11:20:32.661+08:00  INFO 15784 --- [cook-agent] [   ai-advisor-1] c.f.c.r.embedding.ZhipuAiEmbeddingModel  : ZhipuAI embedding done: usage={completion_tokens=0, prompt_tokens=11, total_tokens=11}
2026-05-14T11:20:33.582+08:00  INFO 15784 --- [cook-agent] [           main] c.f.cookagent.rag.RagEffectivenessTest   : 问题: 炖菜有什么通用的技巧？
2026-05-14T11:20:33.582+08:00  INFO 15784 --- [cook-agent] [           main] c.f.cookagent.rag.RagEffectivenessTest   : AI 回答: 根据上下文信息，无法总结出炖菜的通用技巧。
原因
```

OK,rag这个部分测试的差不多了，比较算是完整，接下来问一下ai关于rag这一部分还需要做什么

```
## 需求
OK,rag这个部分测试的差不多了，比较算是完整，接下来问一下ai关于rag这一部分还需要做什么
## 之前生成的任务流
### 阶段二：数据层

| 步骤 | 任务                                    |
| :--: | --------------------------------------- |
| 2.1  | ETL — Markdown 解析器（Java 版）        |
| 2.2  | ETL — 数据导入接口 + 批量写入           |
| 2.3  | 向量化 — Embedding 生成 + PGvector 导入 |
| 2.4  | 数据校验 + 覆盖率报告                   |
```

rag先放一边，后面再优化，先实现agent核心

第五轮对话：实现agent核心

```
你是一位专业的程序员，现在请你根据需求，帮我设计方案、人工确认、按需分步骤完成开发、自主执行测试验证、找我验收。
## 需求
这轮必须开启plan模式

至少要完成以下目标：

实现agent核心功能

普通对话（默认带RAG知识库）

对话记忆，这个需要搜索一下如何实现
（spring ai自带基于内存记忆，我们实现一个基于文件的序列化记忆存储）

Tool工具集，至少要有websearch,图片搜索https://www.pexels.com/zh-cn/实现等等

ReAct Agent 执行多个步骤，调用工具，有自主规划能力的智能体(这个部分也可以websearch)


## 任务

| 步骤 | 任务                                                         |
| :--: | ------------------------------------------------------------ |
| 3.1  | Spring AI DeepSeek 对话通路                                  |
| 3.2  | Tool 工具集（searchRecipe / reverseSearch / recommend / compare） |
| 3.3  | ReAct Agent + 对话记忆                                       |

## 注意
（1）如果你要编写代码，请一定要使用context7获取最新的文档
（2）你帮我整理文档，保存在/docs/ai-docs/{Agent开发}.md
（3）plan计划文档一定我找我却认，怎么选择技术栈
（4）先不实现controller层，先把agent测试完毕后再进行实现
（5）代码分成两部分一个普通对话（默认带RAG知识库）和自主规划能力的智能体，app/agent 和 app/chat
（6）JAVA_HOME="C:/Users/11695/.jdks/ms-21.0.10"
```

多轮记忆对话没有用

```
多轮对话记忆未生效
这是我以前的实现的基于Kryo序列化记忆
package com.fontal.fontalagent.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于文件持久化的对话记忆仓库
 * <p>
 * 实现了 ChatMemoryRepository 接口，配合 MessageWindowChatMemory 使用
 * 滑动窗口的裁剪逻辑由 MessageWindowChatMemory 负责，本类只负责读写
 */
@Component
public class FileBasedChatMemoryRepository implements ChatMemoryRepository {

    private final String BASE_DIR;
    private static final Kryo kryo = new Kryo();

    static {
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }

    public FileBasedChatMemoryRepository(@Value("${app.memory.dir:./chat-memory}") String dir) {
        this.BASE_DIR = dir;
        File baseDir = new File(dir);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    @Override
    public List<String> findConversationIds() {
        File dir = new File(BASE_DIR);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".kryo"));
        if (files == null) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (File file : files) {
            ids.add(file.getName().replace(".kryo", ""));
        }
        return ids;
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        File file = getConversationFile(conversationId);
        if (!file.exists()) {
            return List.of();
        }
        try (Input input = new Input(new FileInputStream(file))) {
            return kryo.readObject(input, ArrayList.class);
        } catch (IOException e) {
            return List.of();
        }
    }

    /**
     * 全量覆盖保存 —— 窗口裁剪由 MessageWindowChatMemory 负责
     * MessageWindowChatMemory 调用此方法时传入的已经是裁剪后的消息列表
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        try (Output output = new Output(new FileOutputStream(file))) {
            kryo.writeObject(output, new ArrayList<>(messages));
        } catch (IOException e) {
            throw new RuntimeException("保存对话记忆失败: " + conversationId, e);
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        File file = getConversationFile(conversationId);
        if (file.exists()) {
            file.delete();
        }
    }

    private File getConversationFile(String conversationId) {
        return new File(BASE_DIR, conversationId + ".kryo");
    }
}

```

存在问题

```
对话存在问题：
    @Test
    @DisplayName("多轮对话记忆 — 记住用户名")
    void multiTurnMemory() {
        String convId = "test-memory-1";

        // 第一轮：告诉 AI 我的名字
        String reply1 = chatService.chat(convId, "我叫小明，我喜欢吃辣的菜");
        assertThat(reply1).isNotBlank();

        // 第二轮：问 AI 我叫什么（应该记住）
        String reply2 = chatService.chat(convId, "我叫什么名字？我喜欢什么口味的菜？");
        assertThat(reply2).isNotBlank();
        // 回答中应包含"小明"（或至少体现了记忆）
    }
第二轮：reply：我不知道
说明ai没有记忆未加载
chat-memory/test-memory-1.kryo
 org.springframework.ai.chat.messages.UserMessag?java.util.ArrayLis?java.util.HashMa?messageTyp?org.springframework.ai.chat.messages.MessageTyp?嶆垜鍙皬鏄庯紝鎴戝枩娆㈠悆杈ｇ殑鑿?org.springframework.ai.chat.messages.AssistantMessag?rol?ASSISTAN?finishReaso?STO?refusa??inde? annotation?java.util.ImmutableCollections$List?i??2ef9716-2044-4fb8-be70-f612c21eb200?鏍规嵁鎻愪緵鐨勮彍鍗曚俊鎭紝鏈夊嚑閬撹彍鍙兘閫傚悎浣狅細鑾寸瑡锛堝惈椴滅孩灏忕背杈ｏ級銆侀骞诧紙鍚矞绾㈠皬绫宠荆鍜岄潚妞掞級銆佷互鍙婂惈鏈夋柊涓€浠ｈ荆妞掋€佷笁妯辨杈ｆ銆佽殨璞嗚荆閰便€佺伀閿呭簳鏂欑殑鑿滃搧锛堝叿浣撻厤姣旀湭鍏竷锛岄渶鑷璋冩暣锛夈€傝繖浜涜彍閮藉寘鍚荆鍛虫垚鍒嗭紝浣犲彲浠ュ皾璇曘€? 掓垜鍙粈涔堝悕瀛楋紵鎴戝枩娆粈涔堝彛鍛崇殑鑿滐紵
 7b4fb62-fcb5-4821-a5b6-77a383724018嗘垜涓嶇煡閬撱€?
这个对话记忆是存在的，但是第二轮对话是未加载吗？

下面是我的之前能够管理记忆的代码
package com.fontal.fontalagent.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于文件持久化的对话记忆仓库
 * <p>
 * 实现了 ChatMemoryRepository 接口，配合 MessageWindowChatMemory 使用
 * 滑动窗口的裁剪逻辑由 MessageWindowChatMemory 负责，本类只负责读写
 */
@Component
public class FileBasedChatMemoryRepository implements ChatMemoryRepository {

    private final String BASE_DIR;
    private static final Kryo kryo = new Kryo();

    static {
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }

    public FileBasedChatMemoryRepository(@Value("${app.memory.dir:./chat-memory}") String dir) {
        this.BASE_DIR = dir;
        File baseDir = new File(dir);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    @Override
    public List<String> findConversationIds() {
        File dir = new File(BASE_DIR);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".kryo"));
        if (files == null) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (File file : files) {
            ids.add(file.getName().replace(".kryo", ""));
        }
        return ids;
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        File file = getConversationFile(conversationId);
        if (!file.exists()) {
            return List.of();
        }
        try (Input input = new Input(new FileInputStream(file))) {
            return kryo.readObject(input, ArrayList.class);
        } catch (IOException e) {
            return List.of();
        }
    }

    /**
     * 全量覆盖保存 —— 窗口裁剪由 MessageWindowChatMemory 负责
     * MessageWindowChatMemory 调用此方法时传入的已经是裁剪后的消息列表
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        try (Output output = new Output(new FileOutputStream(file))) {
            kryo.writeObject(output, new ArrayList<>(messages));
        } catch (IOException e) {
            throw new RuntimeException("保存对话记忆失败: " + conversationId, e);
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        File file = getConversationFile(conversationId);
        if (file.exists()) {
            file.delete();
        }
    }

    private File getConversationFile(String conversationId) {
        return new File(BASE_DIR, conversationId + ".kryo");
    }
}
注意
将文件名改为 conversationId + ".kryo"
```

查看

````
[],USER,我叫小明，我喜欢吃辣的菜,{messageType=USER},""
[],ASSISTANT,根据提供的菜单信息，有几道菜可能适合你：莴笋（含鲜红小米辣）、香干（含鲜红小米辣和青椒）、以及含有新一代辣椒、三樱椒辣椒、蚕豆辣酱、火锅底料的菜品。这些菜都带有辣味，你可以尝试。,"{role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=STOP, index=0, annotations=[], id=19e2d178-744a-4ce1-af39-e51c3b13a81b}",[]
[],USER,我叫什么名字？我喜欢什么口味的菜？,{messageType=USER},""
[],ASSISTANT,我不知道。,"{role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=STOP, index=0, annotations=[], id=3229be72-b464-4536-ba89-840dfe5d778c}",[]

可能的原因
FileBasedChatMemoryRepository 使用 Kryo 序列化/反序列化 ArrayList<Message> 时，反序列化失败或返回了空列表。日志中每行开头的 [] 很可能表示当前对话的历史消息列表为空，导致第二轮请求时 AI 没有上下文记忆

## 二、Spring AI 记忆体系

### 2.1 三层架构

```
┌─────────────────────────────────────────────────┐
│  第1层: ChatMemory（接口）                       │
│    - 定义 add / get / clear 三个核心方法          │
│    - 纯粹的数据读写接口                           │
│    - 常量: CONVERSATION_ID, DEFAULT_CONVERSATION_ID│
├─────────────────────────────────────────────────┤
│  第2层: MessageWindowChatMemory（策略层）          │
│    - 在 ChatMemory 之上加了滑动窗口策略            │
│    - 控制"保留最近 N 条消息"                      │
├─────────────────────────────────────────────────┤
│  第3层: MessageChatMemoryAdvisor（接入层）         │
│    - 作为 Advisor 挂载到 ChatClient              │
│    - 请求前自动读取历史、响应后自动写入             │
│    - 通过 CONVERSATION_ID 区分不同会话            │
└─────────────────────────────────────────────────┘
```

### 2.2 ChatMemoryRepository 接口（Spring AI 1.0）

持久化能力由 Repository 决定，不是 ChatMemory 本身。

```java
public interface ChatMemoryRepository {
    List<String> findConversationIds();                          // 获取所有会话ID
    List<Message> findByConversationId(String conversationId);   // 获取某会话的消息
    void saveAll(String conversationId, List<Message> messages); // 全量覆盖保存
    void deleteByConversationId(String conversationId);          // 删除某会话
}
```

### 2.3 MessageWindowChatMemory 内部逻辑

```java
// add(conversationId, newMessages) 的大致流程：
allMessages = repository.findByConversationId(conversationId)  // 从持久化层读取全部
allMessages.addAll(newMessages)                               // 追加新消息
windowMessages = allMessages.subList(                          // 应用滑动窗口
    max(0, allMessages.size() - maxMessages),
    allMessages.size()
)
repository.saveAll(conversationId, windowMessages)             // 裁剪后全量覆盖写回
```

**关键理解**：`saveAll` 是全量覆盖，不是追加。窗口裁剪由 `MessageWindowChatMemory` 负责，Repository 只管存什么就存什么。

### 2.4 持久化方案对比

| 方案                                    | 适用场景             | 复杂度 |
| --------------------------------------- | -------------------- | ------ |
| InMemoryChatMemoryRepository            | 开发调试             | 零     |
| FileBasedChatMemoryRepository（自实现） | 单机小项目           | 低     |
| JDBC ChatMemoryRepository               | 生产环境，已有数据库 | 中     |
| Redis ChatMemoryRepository              | 分布式/高并发        | 中     |
我们这里主要实现 FileBasedChatMemoryRepository ，其他的不实现

这是能跑通的代码，你必须参考，简化你的代码:
package com.fontal.fontalagent.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于文件持久化的对话记忆仓库
 * <p>
 * 实现了 ChatMemoryRepository 接口，配合 MessageWindowChatMemory 使用
 * 滑动窗口的裁剪逻辑由 MessageWindowChatMemory 负责，本类只负责读写
 */
@Component
public class FileBasedChatMemoryRepository implements ChatMemoryRepository {

    private final String BASE_DIR;
    private static final Kryo kryo = new Kryo();

    static {
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }

    public FileBasedChatMemoryRepository(@Value("${app.memory.dir:./chat-memory}") String dir) {
        this.BASE_DIR = dir;
        File baseDir = new File(dir);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    @Override
    public List<String> findConversationIds() {
        File dir = new File(BASE_DIR);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".kryo"));
        if (files == null) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (File file : files) {
            ids.add(file.getName().replace(".kryo", ""));
        }
        return ids;
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        File file = getConversationFile(conversationId);
        if (!file.exists()) {
            return List.of();
        }
        try (Input input = new Input(new FileInputStream(file))) {
            return kryo.readObject(input, ArrayList.class);
        } catch (IOException e) {
            return List.of();
        }
    }

    /**
     * 全量覆盖保存 —— 窗口裁剪由 MessageWindowChatMemory 负责
     * MessageWindowChatMemory 调用此方法时传入的已经是裁剪后的消息列表
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        try (Output output = new Output(new FileOutputStream(file))) {
            kryo.writeObject(output, new ArrayList<>(messages));
        } catch (IOException e) {
            throw new RuntimeException("保存对话记忆失败: " + conversationId, e);
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        File file = getConversationFile(conversationId);
        if (file.exists()) {
            file.delete();
        }
    }

    private File getConversationFile(String conversationId) {
        return new File(BASE_DIR, conversationId + ".kryo");
    }
}
````

出现这个问题

```
记忆持久化，出现这个问题:

[],USER,我叫小明，我喜欢吃辣的菜,{messageType=USER},""
[],ASSISTANT,根据提供的菜单信息，有几道菜可能适合你：莴笋（含鲜红小米辣）、香干（含鲜红小米辣和青椒）、以及含有新一代辣椒、三樱椒辣椒、蚕豆辣酱、火锅底料的菜品。这些菜都带有辣味，你可以尝试。,"{role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=STOP, index=0, annotations=[], id=19e2d178-744a-4ce1-af39-e51c3b13a81b}",[]
[],USER,我叫什么名字？我喜欢什么口味的菜？,{messageType=USER},""
[],ASSISTANT,我不知道。,"{role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=STOP, index=0, annotations=[], id=3229be72-b464-4536-ba89-840dfe5d778c}",[]

我觉的是ChatService未正确的使用chatMeroryRepository，




下面这是基于灵积大模型的Chat服务层：
@Component

    public GameCustomerServiceApp(ChatModel dashscopeChatModel, ChatMemoryRepository chatMemoryRepository) {

        // [旧版本 - 内存方式] 服务器重启后对话记忆丢失
//        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
//                .chatMemoryRepository(new InMemoryChatMemoryRepository())
//                .maxMessages(20)
//                .build();

        // [新版本 - 文件持久化方式] 服务器重启后对话记忆保留，配合滑动窗口策略只保留最近20条
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20)
                .build();

        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }
```

重新开始对话

````
## 需求
我之前把一些关于chat的文件删除了，ChatMemoryProperties，FileBasedChatMemory 有点小问题，ChatService 也有问题
重新实现ChatService,带有RAG,支持记忆持久化


## 二、Spring AI 记忆体系

### 2.1 三层架构

```
┌─────────────────────────────────────────────────┐
│  第1层: ChatMemory（接口）                       │
│    - 定义 add / get / clear 三个核心方法          │
│    - 纯粹的数据读写接口                           │
│    - 常量: CONVERSATION_ID, DEFAULT_CONVERSATION_ID│
├─────────────────────────────────────────────────┤
│  第2层: MessageWindowChatMemory（策略层）          │
│    - 在 ChatMemory 之上加了滑动窗口策略            │
│    - 控制"保留最近 N 条消息"                      │
├─────────────────────────────────────────────────┤
│  第3层: MessageChatMemoryAdvisor（接入层）         │
│    - 作为 Advisor 挂载到 ChatClient              │
│    - 请求前自动读取历史、响应后自动写入             │
│    - 通过 CONVERSATION_ID 区分不同会话            │
└─────────────────────────────────────────────────┘
```

### 2.2 ChatMemoryRepository 接口（Spring AI 1.0）

持久化能力由 Repository 决定，不是 ChatMemory 本身。

```java
public interface ChatMemoryRepository {
    List<String> findConversationIds();                          // 获取所有会话ID
    List<Message> findByConversationId(String conversationId);   // 获取某会话的消息
    void saveAll(String conversationId, List<Message> messages); // 全量覆盖保存
    void deleteByConversationId(String conversationId);          // 删除某会话
}
```

### 2.3 MessageWindowChatMemory 内部逻辑

```java
// add(conversationId, newMessages) 的大致流程：
allMessages = repository.findByConversationId(conversationId)  // 从持久化层读取全部
allMessages.addAll(newMessages)                               // 追加新消息
windowMessages = allMessages.subList(                          // 应用滑动窗口
    max(0, allMessages.size() - maxMessages),
    allMessages.size()
)
repository.saveAll(conversationId, windowMessages)             // 裁剪后全量覆盖写回
```

**关键理解**：`saveAll` 是全量覆盖，不是追加。窗口裁剪由 `MessageWindowChatMemory` 负责，Repository 只管存什么就存什么。
我们重写了ChatMemoryRepository
````

OK,查看了一下

```
新增加chatModel,在defaultAdvisors添加MessageChatMemoryAdvisor

package com.fontal.cookagent.config;

import com.fontal.cookagent.app.memory.FileBasedChatMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeepSeekConfig {
    @Bean
    public MessageWindowChatMemory chatMemory(FileBasedChatMemory chatMemory) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemory)
                .maxMessages(20)
                .build();
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel,ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                ).build();
    }
}

然后重新写ChatService
```

新增加基于JDBC MySQL数据方式记忆持久化的方式，默认数据库方式。保留FileBasedChatMemory.java文件

```
## 需求

新增加基于JDBC MySQL数据方式记忆持久化的方式，这个项目默认使用数据库方式。保留FileBasedChatMemory.java文件，但是不使用，kryo序列化文件存储存在一定问题，基于文件的不实现。

## 文档

聊天记忆
本站(springdoc.cn)中的内容来源于 spring.io ，原始版权归属于 spring.io。由 springdoc.cn 进行翻译，整理。可供个人学习、研究，未经许可，不得进行任何转载、商用或与之相关的行为。 商标声明：Spring 是 Pivotal Software, Inc. 在美国以及其他国家的商标。
大语言模型（LLM）本质上是无状态的，这意味着它们不会保留历史交互信息。当需要跨多轮交互保持上下文时，这一特性会带来局限。为此，Spring AI 提供了聊天记忆功能，支持在 LLM 交互过程中存储和检索上下文数据。

ChatMemory 抽象层支持实现多种记忆类型以满足不同场景需求。消息的底层存储由 ChatMemoryRepository 处理，其唯一职责是存储和检索消息。 ChatMemory 实现类可自主决定消息保留策略 — 例如保留最近 N 条消息、按时间周期保留或基于 Token 总量限制保留。

选择记忆类型前，需明确聊天记忆与聊天历史的区别：

聊天记忆：大语言模型在对话过程中保留并用于维持上下文感知的信息。

聊天历史：完整的对话记录，包含用户与模型之间交换的所有消息。

ChatMemory 抽象层专为管理聊天记忆设计，支持存储和检索当前会话相关的上下文消息。但若需完整记录所有历史消息，建议采用其他方案（如基于 Spring Data 实现高效的全量聊天历史存储与检索）。

快速入门
Spring AI 自动配置 ChatMemory Bean 供直接使用。默认采用内存存储（InMemoryChatMemoryRepository）及 MessageWindowChatMemory 实现管理会话历史。若已配置其他 Repository（如 Cassandra/JDBC/Neo4j），则自动切换至对应实现。

@Autowired
ChatMemory chatMemory;
以下章节将详细介绍 Spring AI 中可用的不同记忆类型与 Repository 实现。

记忆类型
ChatMemory 抽象层支持实现多种记忆类型以适应不同场景。记忆类型（Memory Type）的选择将显著影响应用性能与行为特征。本节详解 Spring AI 内置的记忆类型及其特性。

MessageWindowChatMemory
MessageWindowChatMemory 维护固定容量的消息窗口（默认 20 条）。当消息超限时，自动移除较早的对话消息（始终保留系统消息）。

MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
    .maxMessages(10)
    .build();
此为 Spring AI 自动配置 ChatMemory Bean 时采用的默认消息类型。

记忆存储
Spring AI 通过 ChatMemoryRepository 抽象层实现聊天记忆存储。本节介绍内置 Repository 及其用法，同时支持自定义实现。

内存 Repository
InMemoryChatMemoryRepository 基于 ConcurrentHashMap 实现内存存储。

默认情况下，若未配置其他 Repository，Spring AI 将自动配置 InMemoryChatMemoryRepository 类型的 ChatMemoryRepository Bean供直接使用。

@Autowired
ChatMemoryRepository chatMemoryRepository;
如需手动创建 InMemoryChatMemoryRepository，可按如下方式操作：

ChatMemoryRepository repository = new InMemoryChatMemoryRepository();
JdbcChatMemoryRepository
JdbcChatMemoryRepository 是内置的 JDBC 实现，支持多种关系型数据库，适用于需要持久化存储聊天记忆的场景。

首先，在项目中添加以下依赖：

Maven

Gradle

<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-chat-memory-repository-jdbc</artifactId>
</dependency>
Spring AI 为 JdbcChatMemoryRepository 提供自动配置，可直接在应用中使用。

@Autowired
JdbcChatMemoryRepository chatMemoryRepository;

ChatMemory chatMemory = MessageWindowChatMemory.builder()
    .chatMemoryRepository(chatMemoryRepository)
    .maxMessages(10)
    .build();
如需手动创建 JdbcChatMemoryRepository，可通过注入 JdbcTemplate 实例及 JdbcChatMemoryRepositoryDialect 实现：

ChatMemoryRepository chatMemoryRepository = JdbcChatMemoryRepository.builder()
    .jdbcTemplate(jdbcTemplate)
    .dialect(new PostgresChatMemoryDialect())
    .build();

ChatMemory chatMemory = MessageWindowChatMemory.builder()
    .chatMemoryRepository(chatMemoryRepository)
    .maxMessages(10)
    .build();
支持的数据库与方言抽象层
Spring AI 通过方言抽象层支持多种关系型数据库，以下为开箱即用支持的数据库：

PostgreSQL

MySQL / MariaDB

SQL Server

HSQLDB

使用 JdbcChatMemoryRepositoryDialect.from(DataSource) 时可基于 JDBC URL 自动识别正确方言。通过实现 JdbcChatMemoryRepositoryDialect 接口可扩展其他数据库支持。

配置属性
属性

说明

默认值

spring.ai.chat.memory.repository.jdbc.initialize-schema

控制初始化 Schema 的时机。可选值：embedded（默认）、always、never。

embedded

spring.ai.chat.memory.repository.jdbc.schema

用于初始化的 Schema 脚本位置。支持 classpath: URL 及平台占位符。

classpath:org/springframework/ai/chat/memory/repository/jdbc/schema-@@platform@@.sql

spring.ai.chat.memory.repository.jdbc.platform

若初始化脚本中使用 @@platform@@ 占位符，则指定其对应的平台标识。

auto-detected

Schema 初始化
自动配置将在启动时使用特定于供应商的 SQL 脚本创建 SPRING_AI_CHAT_MEMORY 表。默认情况下，仅针对嵌入式数据库（H2、HSQL、Derby 等）执行 Schema 初始化。

可通过 spring.ai.chat.memory.repository.jdbc.initialize-schema 属性控制 Schema 初始化行为：

spring.ai.chat.memory.repository.jdbc.initialize-schema=embedded # Only for embedded DBs (default)
spring.ai.chat.memory.repository.jdbc.initialize-schema=always   # Always initialize
spring.ai.chat.memory.repository.jdbc.initialize-schema=never    # Never initialize (useful with Flyway/Liquibase)
要覆盖默认的 Schema 脚本位置，请使用：

spring.ai.chat.memory.repository.jdbc.schema=classpath:/custom/path/schema-mysql.sql
扩展方言
要新增数据库支持，需实现 JdbcChatMemoryRepositoryDialect 接口并提供消息查询、插入及删除的 SQL 语句。随后可将自定义方言传入 Repository Builder。

ChatMemoryRepository chatMemoryRepository = JdbcChatMemoryRepository.builder()
    .jdbcTemplate(jdbcTemplate)
    .dialect(new MyCustomDbDialect())
    .build();
CassandraChatMemoryRepository
CassandraChatMemoryRepository 基于 Apache Cassandra 实现消息存储，适用于需要高可用、持久化、可扩展及利用 TTL 特性的聊天记忆持久化场景。

CassandraChatMemoryRepository 采用时间序列 Schema，完整记录历史聊天窗口，对合规审计极具价值。建议设置生存时间（如三年）。

使用 CassandraChatMemoryRepository 需先在项目中添加以下依赖：

Maven

Gradle

<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-chat-memory-repository-cassandra</artifactId>
</dependency>
Spring AI 为 CassandraChatMemoryRepository 提供自动配置，可直接在应用中使用。

@Autowired
CassandraChatMemoryRepository chatMemoryRepository;

ChatMemory chatMemory = MessageWindowChatMemory.builder()
    .chatMemoryRepository(chatMemoryRepository)
    .maxMessages(10)
    .build();
如需手动创建 CassandraChatMemoryRepository，可通过配置 CassandraChatMemoryRepositoryConfig 实例实现：

ChatMemoryRepository chatMemoryRepository = CassandraChatMemoryRepository
    .create(CassandraChatMemoryConfig.builder().withCqlSession(cqlSession));

ChatMemory chatMemory = MessageWindowChatMemory.builder()
    .chatMemoryRepository(chatMemoryRepository)
    .maxMessages(10)
    .build();
配置属性
属性

说明

默认值

spring.cassandra.contactPoints

用于集群发现的主机地址（可多个）

127.0.0.1

spring.cassandra.port

Cassandra 生协议连接端口

9042

spring.cassandra.localDatacenter

要连接的 Cassandra 数据中心名称

datacenter1

spring.ai.chat.memory.cassandra.time-to-live

Cassandra中消息的生存时间（TTL）设置

spring.ai.chat.memory.cassandra.keyspace

Cassandra Key 空间名称（keyspace）

springframework

spring.ai.chat.memory.cassandra.messages-column

Cassandra 消息列名

springframework

spring.ai.chat.memory.cassandra.table

Cassandra 表

ai_chat_memory

spring.ai.chat.memory.cassandra.initialize-schema

是否在启动时初始化 Schema 结构

true

Schema 初始化
自动配置将创建 ai_chat_memory 表。

可通过设置 spring.ai.chat.memory.repository.cassandra.initialize-schema 为 false 禁用 Schema 自动初始化。

Neo4jChatMemoryRepository
Neo4jChatMemoryRepository 是内置实现，利用 Neo4j 将聊天消息存储为属性图中的节点与关系，适用于需发挥 Neo4j 图数据库特性的聊天记忆持久化场景。

首先，在项目中添加以下依赖：

Maven

Gradle

<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-chat-memory-repository-neo4j</artifactId>
</dependency>
Spring AI 为 Neo4jChatMemoryRepository 提供自动配置，可直接在应用中使用。

@Autowired
Neo4jChatMemoryRepository chatMemoryRepository;

ChatMemory chatMemory = MessageWindowChatMemory.builder()
    .chatMemoryRepository(chatMemoryRepository)
    .maxMessages(10)
    .build();
如需手动创建 Neo4jChatMemoryRepository，可通过注入 Neo4j Driver 实例实现：

ChatMemoryRepository chatMemoryRepository = Neo4jChatMemoryRepository.builder()
    .driver(driver)
    .build();

ChatMemory chatMemory = MessageWindowChatMemory.builder()
    .chatMemoryRepository(chatMemoryRepository)
    .maxMessages(10)
    .build();
配置属性
属性

说明

默认值

spring.ai.chat.memory.repository.neo4j.sessionLabel

存储对话会话的节点的标签

Session

spring.ai.chat.memory.repository.neo4j.messageLabel

存储信息的节点的标签

Message

spring.ai.chat.memory.repository.neo4j.toolCallLabel

存储工具调用的节点（如 Assistant 信息中）的标签

ToolCall

spring.ai.chat.memory.repository.neo4j.metadataLabel

存储消息元数据的节点的标签

Metadata

spring.ai.chat.memory.repository.neo4j.toolResponseLabel

存储工具响应的节点的标签

ToolResponse

spring.ai.chat.memory.repository.neo4j.mediaLabel

存储消息相关的媒体的节点标签

Media

索引初始化
Neo4j 存储库将自动创建会话 ID 和消息索引的优化索引。若使用自定义标签，系统同样会为这些标签建立索引。虽无需初始化 Schema，但需确保应用可访问 Neo4j 实例。

聊天客户端的 Memory
使用 ChatClient API时，可通过注入 ChatMemory 实现来维护跨多轮交互的会话上下文。

Spring AI 提供多种内置 Advisor，用于按需配置 ChatClient 的记忆行为。

当前版本存在限制：执行工具调用时与大型语言模型交互的中间消息不会存入记忆。该问题将在后续版本修复。如需存储此类消息，请参阅 用户控制工具执行 章节的操作指南。
MessageChatMemoryAdvisor：通过指定 ChatMemory 实现管理会话记忆。每次交互时从记忆库检索历史消息，并将其作为消息集合注入提示词。

PromptChatMemoryAdvisor：基于指定 ChatMemory 实现管理会话记忆。每次交互时从记忆库检索历史对话，并以纯文本形式追加至系统（system）提示词。

VectorStoreChatMemoryAdvisor：通过指定 VectorStore 实现管理会话记忆。每次交互时从向量存储检索历史对话，并以纯文本形式追加至系统（system）消息。

例如，若需结合 MessageWindowChatMemory 与 MessageChatMemoryAdvisor，可按如下方式配置：

ChatMemory chatMemory = MessageWindowChatMemory.builder().build();

ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
    .build();
调用 ChatClient 时，MessageChatMemoryAdvisor 将自动管理记忆存储。系统会根据指定的会话 ID 从记忆库检索历史对话：

String conversationId = "007";

chatClient.prompt()
    .user("Do I have license to code?")
    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
    .call()
    .content();
PromptChatMemoryAdvisor
自定义模板
PromptChatMemoryAdvisor 采用默认模板将检索到的会话记忆增强至系统消息。通过 .promptTemplate() 构建方法注入自定义 PromptTemplate 对象可覆盖该行为。

此处提供的 PromptTemplate 用于定制 Advisor 如何将检索到的记忆与系统消息合并。这与在 ChatClient 上配置 TemplateRenderer（通过 .templateRenderer()）不同 — 后者影响 Advisor 运行前初始 user/system 提示内容的渲染。客户端级模板渲染详见 ChatClient提示模板 章节。
自定义 PromptTemplate 可采用任何 TemplateRenderer 实现（默认使用基于 StringTemplate 引擎的 StPromptTemplate）。关键要求是模板必须包含以下两个占位符：

接收原始系统消息的指令（instructions）占位符。

接收检索到的会话记忆（memory）的记忆占位符。

VectorStoreChatMemoryAdvisor
自定义模板
VectorStoreChatMemoryAdvisor 通过默认模板将检索到的会话记忆增强至系统消息。通过 .promptTemplate() 构建方法注入自定义 PromptTemplate 对象可覆盖该行为。

此处提供的P romptTemplate 专门用于配置 Advisor 如何整合检索记忆与系统消息，这与 ChatClient 自身的 TemplateRenderer 配置（通过 .templateRenderer()）有本质区别 — 后者控制 Advisor 执行前原始 user/system 提示内容的渲染逻辑。客户端级模板渲染详见 ChatClient提示模板 章节说明。
自定义 PromptTemplate 可采用任意 TemplateRenderer 实现（默认使用基于 StringTemplate 引擎的 StPromptTemplate）。模板必须包含以下两个占位符：

接收原始系统消息的 instructions 占位符。

接收检索到的长期会话记忆的 long_term_memory 占位符。

聊天模型中的记忆
若直接使用 ChatModel 而非 ChatClient，需显式管理记忆存储：

// 创建 memory 实例
ChatMemory chatMemory = MessageWindowChatMemory.builder().build();
String conversationId = "007";

// 首次交互
UserMessage userMessage1 = new UserMessage("My name is James Bond");
chatMemory.add(conversationId, userMessage1);
ChatResponse response1 = chatModel.call(new Prompt(chatMemory.get(conversationId)));
chatMemory.add(conversationId, response1.getResult().getOutput());

// 第二次交互
UserMessage userMessage2 = new UserMessage("What is my name?");
chatMemory.add(conversationId, userMessage2);
ChatResponse response2 = chatModel.call(new Prompt(chatMemory.get(conversationId)));
chatMemory.add(conversationId, response2.getResult().getOutput());

// 响应会包含 "James Bond"
```

现在记忆是配好了的，但是加上retrievalAdvisor

当PGvector不到时，ai会直接回答不知道

例如：

```
// 第一轮：告诉 AI 我的名字
String reply1 = chatService.chat(convId, "我叫小明，我喜欢吃辣的菜");
assertThat(reply1).isNotBlank();
//reply1:根据提供的菜单信息，有几道菜可能适合你：莴笋（含鲜红小米辣）、香干（含鲜红小米辣和青椒）、以及含有新一代辣椒、三樱椒辣椒、蚕豆辣酱、火锅底料的菜品。这些菜都带有辣味，你可以尝试。
// 第二轮：问 AI 我叫什么（应该记住）
String reply2 = chatService.chat(convId, "我叫什么名字？我喜欢什么口味的菜？");
assertThat(reply2).isNotBlank();
//reply2:我不知道
```

这种情况该怎么解决？



````
你是一位专业的程序员，现在请你根据需求，帮我设计方案、人工确认、按需分步骤完成开发、自主执行测试验证、找我验收。
## 需求

现在记忆是配好了的，测试chatService时但加上retrievalAdvisor

查一些与rag无关的信息，ai会直接回答不知道

例如：

```
// 第一轮：告诉 AI 我的名字
String reply1 = chatService.chat(convId, "我叫小明，我喜欢吃辣的菜");
assertThat(reply1).isNotBlank();
//reply1:根据提供的菜单信息，有几道菜可能适合你：莴笋（含鲜红小米辣）、香干（含鲜红小米辣和青椒）、以及含有新一代辣椒、三樱椒辣椒、蚕豆辣酱、火锅底料的菜品。这些菜都带有辣味，你可以尝试。
// 第二轮：问 AI 我叫什么（应该记住）
String reply2 = chatService.chat(convId, "我叫什么名字？我喜欢什么口味的菜？");
assertThat(reply2).isNotBlank();
//reply2:我不知道
```

这种情况该怎么解决？

我是这样想的，程序首先查PGvector,获取相关的文档，把相关的文档发给ai，ai只总结rag中的文档，但是没有systemprompt的引导，加上一个简单的清晰的prompt，减少没有必要的token。


## 注意
（1）如果你要编写代码，请一定要使用context7获取最新的文档
（2）允许上网搜索解决方案
（3）如果是其它情况请你告诉我
````

我新增加了agent，基于base-react-toolcalling-cookmanus

现在，你只需要细微的改动提示词和检查agent的代码错误和tools

```
你是一位专业的程序员，现在请你根据需求，帮我设计方案、人工确认、按需分步骤完成开发、自主执行测试验证、找我验收。
## 需求

我新增加了agent，基于base-react-toolcalling-cookmanus

现在，你只需要细微的改动提示词和检查agent的代码错误和tools
并且增加测试tool工具测试类和agent测试类
每个tool至少有一个测试类，agent有两道三个测试

## 注意
（1）如果你要编写代码，请一定要使用context7获取最新的文档
```

