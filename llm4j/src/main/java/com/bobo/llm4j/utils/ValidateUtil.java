package com.bobo.llm4j.utils;

/**
 * @Author cly
 * @Description 用于验证、处�?
 * @Date 2024/9/19 14:40
 */
public class ValidateUtil {

    public static String concatUrl(String... params){
        if(params.length == 0) {
            throw new IllegalArgumentException("url params is empty");
        }

        // 拼接字符�?
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            if (params[i].startsWith("/")) {
                params[i] = params[i].substring(1);
            }
            if(params[i].startsWith("?") || params[i].startsWith("&")){
                // 如果sb的末尾是�?”，则删除末�?
                if(sb.length() > 0 && sb.charAt(sb.length()-1) == '/') {
                    sb.deleteCharAt(sb.length() - 1);
                }

            }
            sb.append(params[i]);
            if(!params[i].endsWith("/")){
                sb.append('/');
            }
        }

        // 去掉最后一�?
        if(sb.length() > 0 && sb.charAt(sb.length()-1) == '/'){
            sb.deleteCharAt(sb.length()-1);
        }
        return sb.toString();
    }

}
