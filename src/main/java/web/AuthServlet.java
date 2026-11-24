package web;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import security.AccountStore;
import security.AppAccount;
import java.io.IOException;

@WebServlet(urlPatterns="/api/auth/*")
public class AuthServlet extends HttpServlet {
 private static final long serialVersionUID=1L;
 @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws IOException {
  if(!"/session".equals(req.getPathInfo())){send(resp,404,"{\"error\":\"Auth route not found\"}");return;}
  HttpSession session=req.getSession(false);AppAccount account=session==null?null:(AppAccount)session.getAttribute("campuspark.account");
  send(resp,200,account==null?"{\"authenticated\":false}":accountJson(account));
 }
 @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws IOException {
  String path=req.getPathInfo();
  if("/login".equals(path)){
   try {
    AppAccount account=AccountStore.authenticate(req.getParameter("username"),req.getParameter("password"));
    if(account==null){send(resp,401,"{\"error\":\"Username or password is incorrect.\"}");return;}
    HttpSession old=req.getSession(false);if(old!=null)old.invalidate();
    HttpSession session=req.getSession(true);session.setMaxInactiveInterval(1800);session.setAttribute("campuspark.account",account);
    String secure=req.isSecure()||"https".equalsIgnoreCase(first(req.getHeader("X-Forwarded-Proto")))?"; Secure":"";
    resp.addHeader("Set-Cookie","JSESSIONID="+session.getId()+"; Path="+req.getContextPath()+"; HttpOnly; SameSite=Strict"+secure);
    send(resp,200,accountJson(account));
   }catch(Exception ex){getServletContext().log("CampusPark sign-in failed.",ex);send(resp,503,"{\"error\":\"Sign-in is temporarily unavailable.\"}");}return;
  }
  if("/logout".equals(path)){
   HttpSession session=req.getSession(false);if(session!=null)session.invalidate();
   String secure=req.isSecure()||"https".equalsIgnoreCase(first(req.getHeader("X-Forwarded-Proto")))?"; Secure":"";
   resp.addHeader("Set-Cookie","JSESSIONID=; Path="+req.getContextPath()+"; Max-Age=0; HttpOnly; SameSite=Strict"+secure);
   send(resp,200,"{\"ok\":true}");return;
  }
  send(resp,404,"{\"error\":\"Auth route not found\"}");
 }
 private String accountJson(AppAccount account){return "{\"authenticated\":true,\"username\":\""+quote(account.getUsername())+"\",\"role\":\""+quote(account.getRole())+"\"}";}
 private String quote(String value){return value.replace("\\","\\\\").replace("\"","\\\"");}
 private String first(String value){if(value==null)return null;int comma=value.indexOf(',');return(comma<0?value:value.substring(0,comma)).trim();}
 private void send(HttpServletResponse resp,int status,String body)throws IOException{resp.setStatus(status);resp.setContentType("application/json");resp.setCharacterEncoding("UTF-8");resp.setHeader("Cache-Control","no-store");resp.getWriter().write(body);}
}
