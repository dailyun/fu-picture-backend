package com.dali.yupicturebackend.manager.websocket.disruptor;

import com.dali.yupicturebackend.manager.websocket.model.PictureEditRequestMessage;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;
import com.dali.yupicturebackend.model.entity.User;
@Data
public class PictureEditEvent {

    /**
     * 消息
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 当前用户的 session
     */
    private WebSocketSession session;
    
    /**
     * 当前用户
     */
    private User user;

    /**
     * 图片 id
     */
    private Long pictureId;

}
