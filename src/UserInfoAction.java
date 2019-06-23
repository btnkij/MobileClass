
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import util.DatabaseHelper;
import util.MD5Util;
import util.QueryBuilder;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.management.Query;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.xml.crypto.Data;
import java.util.regex.*;

/**
 * Created by silenus on 2019/6/18.
 */
@WebServlet("/UserInfoAction")
public class UserInfoAction extends HttpServlet
{
    private static QueryBuilder queryBuilder = new QueryBuilder("userinfo");
    private static JSONArray result = new JSONArray();
    private static boolean hasResult = false;

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        System.out.println("UserInfoAction action="+action);
        try
        {
            switch(action)
            {
                case "getResult":
                    getResult(request, response);
                    break;
                case "query":
                    doQuery(request, response);
                    break;
                case "delete":
                    Delete(request, response);
                    break;
                default:
                    throw new Exception("未知的请求类型");
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void getResult(HttpServletRequest request, HttpServletResponse response) throws JSONException, SQLException, IOException {
        System.out.println("enter UserInfoAction.getResult");
        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();
        String sql=queryBuilder.getSelectStmt();
        DatabaseHelper db=new DatabaseHelper();
        ResultSet rs=db.executeQuery(sql);
        processResult(rs);
        out.print(result);
        out.flush();
        out.close();
    }

    private void doQuery(HttpServletRequest request, HttpServletResponse response) throws JSONException, SQLException, IOException{
        System.out.println("enter UserInfoAction.doQuery");
        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();
        queryBuilder.clear();
        queryBuilder.setUsername(request.getParameter("username"));
        queryBuilder.setEmail(request.getParameter("email"));
        queryBuilder.setGender(request.getParameter("gender"));
        queryBuilder.setFullname(request.getParameter("fullname"));
        queryBuilder.setNativeplace(request.getParameter("nativeplace"));
        queryBuilder.setPhone(request.getParameter("phone"));
        String sql=queryBuilder.getSelectStmt();
        DatabaseHelper db=new DatabaseHelper();
        ResultSet rs=db.executeQuery(sql);
        processResult(rs);
        JSONObject json = new JSONObject();
        json.put("errno", 0);
        out.print(json);
        out.flush();
        out.close();
    }

    private void Delete(HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("enter UserInfoAction.doQuery");
        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();
        QueryBuilder q=new QueryBuilder(queryBuilder.getTablename());
        q.setGuid(request.getParameter("guid"));
        if(q.getGuid()==-1){
            throw new Exception("UserInfoAction.Delete: guid为空");
        }
        String sql=q.getDeleteStmt();
        DatabaseHelper db=new DatabaseHelper();
        db.execute(sql);
        JSONObject json = new JSONObject();
        json.put("errno", 0);
        out.print(json);
        out.flush();
        out.close();
    }

    private void processResult(ResultSet rs) throws JSONException, SQLException {
        result = new JSONArray("[]");
        rs.beforeFirst();
        while(rs.next())
        {
            JSONObject item = new JSONObject();
            item.put("guid", rs.getInt("guid"));
            item.put("create_time", rs.getString("create_time"));
            item.put("modify_time", rs.getString("modify_time"));
            item.put("authorization", rs.getInt("authorization"));
            item.put("username", rs.getString("username"));
            item.put("fullname", rs.getString("fullname"));
            item.put("gender", rs.getInt("gender"));
            item.put("schoolnum", rs.getString("schoolnum"));
            item.put("nativeplace", rs.getString("nativeplace"));
            item.put("email", rs.getString("email"));
            item.put("phone", rs.getString("phone"));
            result.put(item);
        }
        hasResult=true;
    }

}