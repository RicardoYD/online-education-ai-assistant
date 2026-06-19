package com.tianji.aigc.tools;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.tianji.aigc.config.ToolResultHolder;
import com.tianji.aigc.constants.Constant;
import com.tianji.aigc.tools.result.CourseInfo;
import com.tianji.api.client.course.CourseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CourseTools {

    private final CourseClient courseClient;
    private static final String FIELD_NAME_FORMAT = "{}_{}";

    /**
     * 根据课程id查询课程信息
     *
     * @param courseId 课程id
     * @return 课程信息
     */
    @Tool(description = Constant.Tools.QUERY_COURSE_BY_ID)
    public CourseInfo queryCourseById(@ToolParam(description = Constant.ToolParams.COURSE_ID) Long courseId, ToolContext toolContext) {
        return Optional.ofNullable(courseId)
                .map(id -> CourseInfo.of(this.courseClient.baseInfo(id, true)))
                .map(courseInfo -> {
                    // 添加课程id到工具上下文，用于后续的调用
                    var requestId = MapUtil.get(toolContext.getContext(), Constant.REQUEST_ID, String.class);
                    var field = StrUtil.format(FIELD_NAME_FORMAT, StrUtil.lowerFirst(CourseInfo.class.getSimpleName()), courseId);
                    ToolResultHolder.put(requestId, field, courseInfo);
                    return courseInfo;
                })
                .orElse(null);
    }
}