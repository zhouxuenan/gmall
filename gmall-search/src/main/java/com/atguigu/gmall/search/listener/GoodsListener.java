package com.atguigu.gmall.search.listener;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValue;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GoodsListener {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GoodsRepository goodsRepository;


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "SEARCH_SAVE_QUEUE", durable = "true"),
            exchange = @Exchange(
                    value = "PMS_SPU_EXCHANGE",
                    ignoreDeclarationExceptions = "true",
                    type = ExchangeTypes.TOPIC),
            key = {"item.insert"}))

    public void listenCreate(Long spuId, Channel channel, Message message) throws IOException {

        try {
            ResponseVo<List<SkuEntity>> skuResp = this.pmsClient.list(spuId);
            List<SkuEntity> skuEntities = skuResp.getData();
            if (!CollectionUtils.isEmpty(skuEntities)){
                //把sku转化成goods对象
                List<Goods> goodsList = skuEntities.stream().map(skuEntity -> {
                    Goods goods = new Goods();
                    //查询spu搜索属性及值
                    ResponseVo<List<SpuAttrValueEntity>> attrValueResp = this.pmsClient.querySearchAttrValueBySpuId(spuId);
                    List<SpuAttrValueEntity> attrValueEntities = attrValueResp.getData();
                    List<SearchAttrValue> searchAttrValues = new ArrayList<>();
                    if(!CollectionUtils.isEmpty(attrValueEntities)){
                        searchAttrValues = attrValueEntities.stream().map(spuAttrValueEntity -> {
                            SearchAttrValue searchAttrValue = new SearchAttrValue();
                            searchAttrValue.setAttrId(spuAttrValueEntity.getAttrId());
                            searchAttrValue.setAttrName(spuAttrValueEntity.getAttrName());
                            searchAttrValue.setAttrValue(spuAttrValueEntity.getAttrValue());
                            return searchAttrValue;
                        }).collect(Collectors.toList());
                    }
//                        goods.setCreateTime(spuEntity.getCreateTime());

                    // 查询sku搜索属性及值
                    ResponseVo<List<SkuAttrValueEntity>> skuAttrValueResp = this.pmsClient.querySearchAttrValueBySkuId(spuId);
                    List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueResp.getData();
                    List<SearchAttrValue> searchSkuAttrValues = new ArrayList<>();
                    if(!CollectionUtils.isEmpty(skuAttrValueEntities)){
                        searchSkuAttrValues = skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                            SearchAttrValue searchAttrValue = new SearchAttrValue();
                            searchAttrValue.setAttrId(skuAttrValueEntity.getAttrId());
                            searchAttrValue.setAttrName(skuAttrValueEntity.getAttrName());
                            searchAttrValue.setAttrValue(skuAttrValueEntity.getAttrValue());
                            return searchAttrValue;
                        }).collect(Collectors.toList());
                    }
                    searchAttrValues.addAll(searchSkuAttrValues);
                    goods.setSearchAttrs(searchAttrValues);
//                        goods.setSkuId(skuEntity.getId());
//                        goods.setTitle(skuEntity.getTitle());
                    goods.setSubTitle(skuEntity.getSubtitle());
//                        goods.setPrice(skuEntity.getPrice().doubleValue());
//                        goods.setDefaultImage(skuEntity.getDefaultImage());

                    //查询品牌
                    ResponseVo<BrandEntity> brandEntityResp = this.pmsClient.queryBrandById(skuEntity.getBrandId());
                    BrandEntity brandEntity = brandEntityResp.getData();
                    if(brandEntity != null){
                        goods.setBrandId(skuEntity.getBrandId());
//                            goods.setBrandId(brandEntity.getId());
                        goods.setBrandName(brandEntity.getName());
                        goods.setLogo(brandEntity.getLogo());
                    }

                    //查询分类
                    ResponseVo<CategoryEntity> categoryEntityResp = this.pmsClient.queryCategoryById(skuEntity.getCatagoryId());
                    CategoryEntity categoryEntity = categoryEntityResp.getData();
                    if(categoryEntity != null){
                        goods.setCategoryId(skuEntity.getCatagoryId());
//                            goods.setCategoryId(categoryEntity.getId());
                        goods.setCategoryName(categoryEntity.getName());
                    }

                    ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(spuId);
                    SpuEntity spuEntity = spuEntityResponseVo.getData();
                    if(spuEntity != null){
                        goods.setCreateTime(spuEntity.getCreateTime());
                    }

                    goods.setDefaultImage(skuEntity.getDefaultImage());
                    goods.setPrice(skuEntity.getPrice().doubleValue());
                    goods.setSales(0l);
                    goods.setSkuId(skuEntity.getId());

                    //查询库存信息
                    ResponseVo<List<WareSkuEntity>> listResp = this.wmsClient.queryWareSkuBySkuId(skuEntity.getId());
                    List<WareSkuEntity> wareSkuEntities = listResp.getData();
                    if(!CollectionUtils.isEmpty(wareSkuEntities)){
                        boolean flag = wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0);
                        goods.setStore(flag);
//                            goods.setSales(0l);
                    }
//                        goods.setSearchAttrs(searchAttrValues);
                    goods.setTitle(skuEntity.getTitle());
                    return goods;
                }).collect(Collectors.toList());

                //导入索引库
                this.goodsRepository.saveAll(goodsList);
            }
            System.out.println(spuId);
            // 手动确认消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
            // 是否已经重试过
            if (message.getMessageProperties().getRedelivered()){
                // 已重试过直接拒绝
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            } else {
                // 未重试过，重新入队
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            }
        }
    }
}