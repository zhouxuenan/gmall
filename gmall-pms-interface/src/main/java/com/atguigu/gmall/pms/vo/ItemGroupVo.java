package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.AttrEntity;
import lombok.Data;

import java.util.List;

@Data
public class ItemGroupVo {
    private Long groupId;
    private String groupName;
    private List<AttrValueVo> attrs;
//    private List<AttrEntity> attrEntities;

//    public void setAttrEntities(List<AttrEntity> attrEntities) {
//    }
}
