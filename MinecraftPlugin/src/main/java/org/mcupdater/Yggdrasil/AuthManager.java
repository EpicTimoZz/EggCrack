/*     */ package org.mcupdater.Yggdrasil;
/*     */ 
/*     */ import com.google.gson.Gson;
/*     */ import java.io.DataOutputStream;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.net.HttpURLConnection;
/*     */ import java.net.MalformedURLException;
/*     */ import java.net.Proxy;
/*     */ import java.net.URL;
/*     */ import java.nio.charset.Charset;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class AuthManager
/*     */ {
/*  21 */   private final Agent MINECRAFT = new Agent("Minecraft", 1);
/*  22 */   private final URL AUTH = constantURL("https://authserver.mojang.com/authenticate");
/*  23 */   private final URL REFRESH = constantURL("https://authserver.mojang.com/refresh");
/*  24 */   private final Gson gson = new Gson();
/*     */   
/*     */   public Gson getGson() {
/*  27 */     return this.gson;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public String authenticate(String username, String password, String clientToken, Proxy currentProxy) throws IOException
/*     */   {
/*  41 */     AuthRequest request = new AuthRequest(this.MINECRAFT, username, password, clientToken);
/*  42 */     return performJsonPost(this.AUTH, this.gson.toJson(request), currentProxy);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public SessionResponse refresh(String accessToken, String clientToken) throws IOException
/*     */   {
/*  53 */     RefreshRequest request = new RefreshRequest(accessToken, clientToken);
/*  54 */     String result = performJsonPost(this.REFRESH, this.gson.toJson(request), Proxy.NO_PROXY);
/*  55 */     SessionResponse response = (SessionResponse)this.gson.fromJson(result, SessionResponse.class);
/*  56 */     return response;
/*     */   }
/*     */   
/*     */   private String performJsonPost(URL url, String json, Proxy currentProxy) throws IOException {
/*     */     try {
/*  61 */       HttpURLConnection conn = (HttpURLConnection)url.openConnection(currentProxy);
/*  62 */       byte[] payloadAsBytes = json.getBytes(Charset.forName("UTF-8"));
/*     */       
/*  64 */       conn.setConnectTimeout(1200);
/*  65 */       conn.setReadTimeout(10000);
/*  66 */       conn.setRequestMethod("POST");
/*  67 */       conn.setRequestProperty("User-Agent", "MCU-Yggdrasil/1.0");
/*  68 */       conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
/*  69 */       conn.setRequestProperty("Content-Length", "" + payloadAsBytes.length);
/*  70 */       conn.setRequestProperty("Content-Language", "en-US");
/*  71 */       conn.setUseCaches(false);
/*  72 */       conn.setDoInput(true);
/*  73 */       conn.setDoOutput(true);
/*     */       
/*  75 */       DataOutputStream outStream = new DataOutputStream(conn.getOutputStream());
/*  76 */       outStream.write(payloadAsBytes);
/*  77 */       outStream.flush();
/*  78 */       outStream.close();

                if (conn.getResponseCode() / 100 != 2) throw new IOException(conn.getResponseMessage());
/*     */       
/*  80 */       InputStream inStream = null;
/*     */       try {
/*  82 */         inStream = conn.getInputStream();
/*     */       } catch (Exception e) {
/*  84 */         inStream = conn.getErrorStream();
/*     */       }
/*     */       
/*  87 */       StringBuilder response = new StringBuilder();
/*  88 */       byte[] buffer = new byte[2048];
/*     */       int bytesRead;
/*  90 */       while ((bytesRead = inStream.read(buffer)) > 0) {
/*  91 */         response.append(new String(buffer, "UTF-8").substring(0, bytesRead));
/*     */       }
/*  93 */       return response.toString();
/*     */     }
/*     */     catch (IOException e) {
/*  96 */       throw e;
/*     */     }
/*     */   }
/*     */   
/*     */   private static URL constantURL(String input) {
/*     */     try {
/* 103 */       return new URL(input);
/*     */     } catch (MalformedURLException e) {
/* 105 */       throw new Error(e);
/*     */     }
/*     */   }
/*     */ }