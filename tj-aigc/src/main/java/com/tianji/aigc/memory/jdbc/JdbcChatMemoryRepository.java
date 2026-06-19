package com.tianji.aigc.memory.jdbc;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tianji.aigc.entity.ChatRecord;
import com.tianji.aigc.memory.MessageUtil;
import com.tianji.aigc.service.ChatRecordService;
import jakarta.annotation.Resource;
import kotlin.jvm.internal.Lambda;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 基于JDBC的会话记忆，聊天记忆保存到数据库中
 */
public class JdbcChatMemoryRepository implements ChatMemoryRepository {

    @Resource
    private ChatRecordService chatRecordService;

    @Override
    public List<String> findConversationIds() {
        var chatRecordList = this.chatRecordService.lambdaQuery()
                .select(ChatRecord::getConversationId)
                .list();
        return CollStreamUtil.toList(chatRecordList, ChatRecord::getConversationId);
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        var chatRecordList = this.chatRecordService.lambdaQuery()
                .eq(ChatRecord::getConversationId, conversationId)
                .orderByAsc(ChatRecord::getCreateTime)
                .list();
        return CollStreamUtil.toList(chatRecordList, chatRecord-> MessageUtil.toMessage(chatRecord.getData()));
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        this.deleteByConversationId(conversationId);
        var userId = Convert.toLong(StrUtil.subBefore(conversationId,'_', true));

        var chatRecordList = CollStreamUtil.toList(messages, message -> ChatRecord.builder()
                .data(MessageUtil.toJson(message))
                .conversationId(conversationId)
                .creater(userId)
                .updater(userId)
                .build());
        this.chatRecordService.saveBatch(chatRecordList);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        var queryWrapper = Wrappers.<ChatRecord>lambdaQuery()
                .eq(ChatRecord::getConversationId, conversationId);
        this.chatRecordService.remove(queryWrapper);
    }
}
