package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

//接受页面传递过来的检索参数
@Data
public class SearchParamVo {
    //检索条件
    private String keyword;
    //品牌过滤
    private List<Long> brandId;
    //分类过滤
    private Long cid;
    //过滤的检索参数
    private List<String> props;
    // 排序字段：0-默认，得分降序；1-按价格升序；2-按价格降序；3-按创建时间降序；4-按销量降序
    private Integer sort = 0;
    //价格区间
    private Double priceFrom;
    private Double priceTo;

    //页码
    private Integer pageNum = 1;
    //每页记录数
    private final Integer pageSize = 20;

    //是否有货
    private Boolean store;

}
