package com.atguigu.gmall.auth;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    // 别忘了创建D:\\project\rsa目录
	private static final String pubKeyPath = "E:\\MyWork\\project\\rsa\\rsa.pub";
    private static final String priKeyPath = "E:\\MyWork\\project\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "234");
    }

    @BeforeEach
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 2);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE2MDU2MDkwNDZ9.JQ7IJFd7dY9BvmntNJoW6qjEqbMCPXk4tf8dvrPPVHJHIeoOe60xz3KZxbwyQ0QTWOb3JukZk5K-1NV3Bg1DEP2PPUCVWG4iARa-Up8ynMR65Z7FuCYBpDAInknB9RNWHO0jeUXxlyTGXJLoM2B7E0PE1Wc-fXh0BSgsGAGqEFTsLRyF0xo1S-Iam4cK1p-Fg-WfpFsA_h1duDUlSzt6EIcuwEEmmPzxrtXm142C9xTPnF7_T0msFWJraX899nOkKnXOKkukm5F9x99tvrmcmw2hr1Fa10bG8PRXvucdJ_79XyTFXZJWLrL_KCr3IbS81H5mkIKiW-dBYorTvNMygw";
        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}