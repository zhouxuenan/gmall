package com.atguigu.gmall.pms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import org.springframework.util.CollectionUtils;


@Service("skuAttrValueService")
public class SkuAttrValueServiceImpl extends ServiceImpl<SkuAttrValueMapper, SkuAttrValueEntity> implements SkuAttrValueService {

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuMapper skuMapper;


    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuAttrValueEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuAttrValueEntity>()
        );
        return new PageResultVo(page);
    }

    @Override
    public List<SkuAttrValueEntity> querySearchAttrValueBySkuId(Long skuId) {
        return this.skuAttrValueMapper.querySearchAttrValueBySkuId(skuId);

    }

    @Override
    public List<SaleAttrValueVo> querySaleAttrValuesBySpuId(Long spuId) {
        List<SkuEntity> skuEntities = this.skuMapper.selectList(new QueryWrapper<SkuEntity>().eq("spu_id", spuId));
        if(CollectionUtils.isEmpty(skuEntities)){
            return null;
        }
        List<Long> skuIds = skuEntities.stream().map(SkuEntity::getId).collect(Collectors.toList());
        //查询sku对应的销售属性
        List<SkuAttrValueEntity> skuAttrValueEntities = this.list(new QueryWrapper<SkuAttrValueEntity>().in("sku_id", skuIds));
        if(CollectionUtils.isEmpty(skuAttrValueEntities)){
            return null;
        }
        //对所有销售属性以attrId分组
        //分组后的map是以attrId作为key,以List<SkuAttrValueEntity>作为value
        Map<Long, List<SkuAttrValueEntity>> map = skuAttrValueEntities.stream().collect(Collectors.groupingBy(SkuAttrValueEntity::getAttrId));
        List<SaleAttrValueVo> attrValueVos = new ArrayList<>();
        map.forEach((attrId,attrValueEntities) -> {
            SaleAttrValueVo saleAttrValueVo = new SaleAttrValueVo();
            saleAttrValueVo.setAttrId(attrId);
            if(!CollectionUtils.isEmpty(attrValueEntities)){
                //如果销售属性集合不为空，取第一个元素获取规格参数名
                saleAttrValueVo.setAttrName(attrValueEntities.get(0).getAttrName());
                Set<String> attrValues = attrValueEntities.stream().map(SkuAttrValueEntity::getAttrValue).collect(Collectors.toSet());
                saleAttrValueVo.setAttrValues(attrValues);
            }
            attrValueVos.add(saleAttrValueVo);
        });
        return attrValueVos;
    }

    @Override
    public String querySaleAttrValuesMappingSkuId(Long spuId){
        List<Map<String, Object>> maps = this.skuAttrValueMapper.querySaleAttrValuesMappingSkuId(spuId);
        if(CollectionUtils.isEmpty(maps)){
            return null;
        }
        Map<String, Long> collect = maps.stream().collect(Collectors.toMap(map -> map.get("attr_values").toString(), map -> (Long) map.get("sku_id")));
        return JSON.toJSONString(collect);
    }

}