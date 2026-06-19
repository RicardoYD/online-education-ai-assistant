package com.tianji.aigc.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.config.ToolResultHolder;
import com.tianji.aigc.constants.Constant;
import com.tianji.aigc.entity.ChatSession;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.service.ChatSessionService;
import com.tianji.aigc.vo.ChatEventVO;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tj.ai",name = "chat-type", havingValue = "ENHANCE")
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final SystemPromptConfig systemPromptConfig;
    private final ChatMemory chatMemory;
    private final StringRedisTemplate stringRedisTemplate;
    public static final ChatEventVO STOP_EVENT = ChatEventVO.builder().eventType(ChatEventTypeEnum.STOP.getValue()).build();
    // 存储大模型的生成状态，这里采用ConcurrentHashMap是确保线程安全
    // 目前的版本暂时用Map实现，如果考虑分布式环境的话，可以考虑用redis来实现
    //private static final Map<String, Boolean> GENERATE_STATUS = new ConcurrentHashMap<>();
    private static final String GENERATE_STATUS_KEY = "GENERATE_STATUS";
    private final VectorStore vectorStore;
    private final ChatSessionService chatSessionService;




    @Override
    public Flux<ChatEventVO> chat(String question, String sessionId) {
        // 获取对话id
        var conversationId = ChatService.getConversationId(sessionId);
        // 大模型输出内容的缓存器，用于在输出中断后的数据存储
        var outputBuilder = new StringBuilder();
        var hashOps = this.stringRedisTemplate.boundHashOps(GENERATE_STATUS_KEY);
        var requestId = IdUtil.simpleUUID();
        var userId = UserContext.getUser();
        this.chatSessionService.update(sessionId, question, userId);

        //定义RAG增强
        var qaAdvisor = QuestionAnswerAdvisor.builder(this.vectorStore)
                .searchRequest(SearchRequest.builder()
                        .similarityThreshold(0.6d) // 设置相似度阈值
                        .topK(6)
                        .build())
                .build();


        return this.chatClient.prompt()
                .system(promptSystem -> promptSystem
                        .text(this.systemPromptConfig.getChatSystemMessage().get()) // 设置系统提示语
                        .param("now", DateUtil.now()) // 设置当前时间的参数
                )
                .advisors(advisor -> advisor
                        .advisors(qaAdvisor)   // 添加RAG增强
                        .param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolContext(Map.of(Constant.REQUEST_ID,requestId,Constant.USER_ID,userId))
                .user(question)
                .stream()
                .chatResponse()
                .doFirst(() -> hashOps.put(sessionId, "true"))
                .doOnError(throwable -> hashOps.delete(sessionId)) // 错误时清除标记
                .doOnComplete(() -> hashOps.delete(sessionId))
                .doOnCancel(() -> {
                    // 当输出被取消时，保存输出的内容到历史记录中
                    this.saveStopHistoryRecord(conversationId, outputBuilder.toString());
                })
                // 输出过程中，判断是否正在输出，如果正在输出，则继续输出，否则结束输出
                .takeWhile(response -> hashOps.get(sessionId) != null)
                .map(chatResponse -> {
                    var finishReason = chatResponse.getResult().getMetadata().getFinishReason();
                    if (StrUtil.equals(Constant.STOP, finishReason)) {
                        var messageId = chatResponse.getMetadata().getId();
                        ToolResultHolder.put(messageId, Constant.REQUEST_ID, requestId);
                    }
                    // 获取大模型的输出的内容
                    var text = chatResponse.getResult().getOutput().getText();
                    // 追加到输出内容中
                    outputBuilder.append(text);
                    // 封装响应对象
                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                })
                .concatWith(Flux.defer(()->{
                    var result = ToolResultHolder.get(requestId);
                    if(ObjectUtil.isNotEmpty( result)){
                        ToolResultHolder.remove(requestId);
                        return Flux.just(ChatEventVO.builder()
                                        .eventData(result)
                                        .eventType(ChatEventTypeEnum.PARAM.getValue())
                                        .build(),STOP_EVENT);
                    }
                    return Flux.just(STOP_EVENT);
                }));
    }


    @Override
    public void stop(String sessionId) {
        var hashOps = this.stringRedisTemplate.boundHashOps(GENERATE_STATUS_KEY);
        // 移除标记
        hashOps.delete(sessionId);
    }

    /**
     * 保存停止输出的记录
     *
     * @param conversationId 会话id
     * @param content        大模型输出的内容
     */
    private void saveStopHistoryRecord(String conversationId, String content) {
        this.chatMemory.add(conversationId, new AssistantMessage(content));
    }

}
