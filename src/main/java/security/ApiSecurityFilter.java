package security;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;

@WebFilter(urlPatterns="/api/*")
public class ApiSecurityFilter implements Filter {
 public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,ServletException {
  HttpServletRequest req=(HttpServletRequest)request; HttpServletResponse resp=(HttpServletResponse)response;
  String path=req.getPathInfo()==null?"/":req.getPathInfo();
  if(Boolean.FALSE.equals(req.getServletContext().getAttribute("campuspark.security.ready"))){send(resp,503,"Account security is not configured. Contact the administrator.");return;}
  String requestPath=req.getRequestURI().substring(req.getContextPath().length()); if("/api/health".equals(requestPath)||requestPath.startsWith("/api/auth/")){chain.doFilter(request,response);return;}
  HttpSession session=req.getSession(false); AppAccount account=session==null?null:(AppAccount)session.getAttribute("campuspark.account");
  if(account==null){send(resp,401,"Please sign in to view parking information.");return;}
  String method=req.getMethod().toUpperCase(Locale.ROOT);
  boolean write="POST".equals(method)||"PUT".equals(method)||"PATCH".equals(method)||"DELETE".equals(method);
  if(write&&!account.isAdmin()){send(resp,403,"Viewer accounts have read-only access.");return;}
  if(write&&!sameOrigin(req)){send(resp,403,"Request origin check failed.");return;}
  chain.doFilter(request,response);
 }
 private boolean sameOrigin(HttpServletRequest req){
  String origin=req.getHeader("Origin"); if(origin==null)return false;
  try { URI parsed=URI.create(origin); String scheme=first(req.getHeader("X-Forwarded-Proto"));
   if(scheme==null||scheme.isEmpty())scheme=req.isSecure()?"https":"http";
   String host=first(req.getHeader("X-Forwarded-Host"));if(host==null||host.isEmpty())host=req.getHeader("Host");
   return host!=null&&parsed.getScheme()!=null&&parsed.getRawAuthority()!=null&&
    (scheme+"://"+host).equalsIgnoreCase(parsed.getScheme()+"://"+parsed.getRawAuthority());
  }catch(IllegalArgumentException ex){return false;}
 }
 private String first(String value){if(value==null)return null;int comma=value.indexOf(',');return(comma<0?value:value.substring(0,comma)).trim();}
 private void send(HttpServletResponse resp,int status,String message)throws IOException{
  resp.setStatus(status);resp.setContentType("application/json");resp.setCharacterEncoding("UTF-8");resp.setHeader("Cache-Control","no-store");
  resp.getWriter().write("{\"error\":\""+message.replace("\\","\\\\").replace("\"","\\\"")+"\"}");
 }
}
