package com.bobo.llm4j.utils;

/**
 * @Author bo
 * @Description 用于验证
 */
public class ValidateUtil {

    public static String concatUrl(String... params){
        if(params.length == 0) {
            throw new IllegalArgumentException("url params is empty");
        }

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

        if(sb.length() > 0 && sb.charAt(sb.length()-1) == '/'){
            sb.deleteCharAt(sb.length()-1);
        }
        return sb.toString();
    }

}
