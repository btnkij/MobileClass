/**
 * Created by Janspiry on 2019/6/15.
 */

import Questionnaire.QueryBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import util.DatabaseHelper;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

@WebServlet("/QuestionnaireAnswer")
public class QuestionnaireAnswer extends HttpServlet {

    private static JSONArray queryResult = null;
    private static QueryBuilder queryBuilder = null;
    static {
        try {
            queryResult = new JSONArray("[]");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        queryBuilder = new QueryBuilder("questionnaire");
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String action = request.getParameter("action");
        System.out.println(action);
        try
        {
            switch(action)
            {
                case "get_record":
                    getRecord(request, response);
                    break;
                case "add_record":
                    addRecord(request, response);
                    break;
                case "delete_record":
                    deleteRecord(request, response);
                    break;
                case "modify_record":
                    modifyRecord(request, response);
                    break;
                case "getStatistics":
                    getStatistics(request, response);
                    break;

                default:
                    System.out.println("AuthorizationAction.service: invalid action: "+action);
                    break;
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }
    public void addRecord(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        String userId=session.getAttribute("guid")==null?null:(String)session.getAttribute("guid");
        String userName=(String)session.getAttribute("username");
        String changeTime=(new SimpleDateFormat("yyyy-MM-dd")).format(new Date());
        int count =Integer.parseInt(request.getParameter("count"));
        String guid = request.getParameter("id");

        String problemName="problem"+1;
        String answerName="answer"+1;
        String problem=request.getParameter(problemName);
        String answer=request.getParameter(answerName);


        DatabaseHelper db = new DatabaseHelper();
        ResultSet rs;

        String tableName="questionnaire";
        String sql="select * from "+tableName+" where guid="+guid;
        rs = db.executeQuery(sql);
        rs.next();
        String questionnaireId=rs.getString("questionnaire_id");

        sql="select * from "+tableName+" where questionnaire_id="+questionnaireId+" and problem_id=-1";
        rs = db.executeQuery(sql);
        rs.next();
        String title=rs.getString("title");
        String creator=rs.getString("author_name");
        String limitTime=rs.getString("limit_time");
        String createTime=rs.getString("create_time");
        String answer_num=rs.getString("answer_num");
        String status=rs.getString("status");

        //更新答案数量
        int num=Integer.parseInt(answer_num);
        num++;
        sql="update "+tableName+" set answer_num="+num+" where questionnaire_id="+questionnaireId+" and problem_id=-1";
        System.out.println("增加回答数:"+sql);
        db.executeUpdate(sql);

        //插入新回答
        queryBuilder.clear();
        queryBuilder.setTitle(title);
        queryBuilder.setUserId(userId);
        queryBuilder.setUserName(userName);
        queryBuilder.setCreator(creator);
        queryBuilder.setCreateTime(changeTime);
        queryBuilder.setChangeTime(changeTime);
        queryBuilder.setLimitTime(limitTime);
        queryBuilder.setQuestionnaireId(questionnaireId);//问卷id
        queryBuilder.setStatus(status);
        //用户回答此问卷的标识
        queryBuilder.setUserFlag("1");
        queryBuilder.setProblemId("0");
        sql=queryBuilder.getInsertStmt();
        db.execute(sql);

        //用户回答此问卷对应问题的标识
        queryBuilder.setUserFlag("0");
        queryBuilder.setAnsweFlag("1");
        for(int j=1;j<=count;j++){
            problemName="problem"+j;
            answerName="answer"+j;
            request.setCharacterEncoding("utf-8");
            problem=request.getParameter(problemName);
            answer=request.getParameter(answerName);
            String value1 =new String(problem.getBytes("iso-8859-1"),"utf-8");
            String value2 =new String(answer.getBytes("iso-8859-1"),"utf-8");
//            System.out.println("问题"+problem);
//            System.out.println("答案"+answer);
            System.out.println("问题"+value1);
            System.out.println("答案"+value2);
            queryBuilder.setProblem(value1);
            queryBuilder.setAnswer(value2);
            queryBuilder.setProblemId(Integer.toString(j));
            sql=queryBuilder.getInsertStmt();
            db.execute(sql);
        }
        response.sendRedirect("questionnaire/publish/questionnaire_list.jsp");
    }
    private void getRecord(HttpServletRequest request, HttpServletResponse response) throws IOException, JSONException, SQLException {
        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession();
        String id=request.getParameter("id");
        String type= request.getParameter("type");
        String title=request.getParameter("title");
        String author_name=(String)request.getParameter("author_name");
        String orderby=(String)request.getParameter("orderby");

        System.out.println("getResultexist_result=false or null");
        queryBuilder.clear();
        //问卷问题所需
        queryBuilder.setGuid(id);
        queryBuilder.setType(type);


        queryBuilder.setTitle(title);
        queryBuilder.setCreator(author_name);
        queryBuilder.setOrderBy(orderby);

        String sql=queryBuilder.getAnswerSql();
        DatabaseHelper db=new DatabaseHelper();
        ResultSet rs=db.executeQuery(sql);
        processResult(rs);

        for(int i=0;i<queryResult.length();i++)
        {
            System.out.printf("result[%d].guid=%d\n",i, queryResult.getJSONObject(i).getInt("guid"));
        }
        out.print(queryResult);
        out.flush();
        out.close();
        System.out.println("exit getResult");
    }
    private void deleteRecord(HttpServletRequest request, HttpServletResponse response) throws IOException, JSONException, SQLException {
        response.setContentType("application/json; charset=UTF-8");
        HttpSession session = request.getSession();
        String guid=request.getParameter("guid");
        queryBuilder.clear();
        DatabaseHelper db=new DatabaseHelper();

        String tableName="questionnaire";
        String sql="select * from "+tableName+" where guid="+guid;
        ResultSet rs = db.executeQuery(sql);
        String questionId="";
        String userId="";
        while (rs.next()) {
            questionId=rs.getString("questionnaire_id");
            userId=rs.getString("user_id");
        }
        System.out.println("问卷序号:"+questionId);
        sql="delete from "+tableName+" where questionnaire_id="+questionId;
        db.executeUpdate(sql);

    }
    private void modifyRecord(HttpServletRequest request, HttpServletResponse response) throws JSONException, SQLException, IOException, ParseException {
        System.out.println("enter modifyRecord");
        String guid = request.getParameter("guid");
        String limit_time = request.getParameter("limit_time");
        String title = request.getParameter("title");
        String createTime=(new SimpleDateFormat("yyyy-MM-dd")).format(new Date());
        String status="processing";

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        if(format.parse(limit_time).compareTo(format.parse(createTime))<0){
            status="ended";
        }

        String tableName="questionnaire";
        String sql="select * from "+tableName+" where guid="+guid;
        DatabaseHelper db=new DatabaseHelper();
        ResultSet rs = db.executeQuery(sql);
        String questionId="";
        while (rs.next()) {
            questionId=rs.getString("questionnaire_id");
        }
        sql="update "+tableName+" set";
        if(title!=null&&title!=""){
            sql=sql+" title='"+title+"'";
        }
        if(limit_time!=null&&limit_time!=""){
            sql=sql+" ,limit_time='"+limit_time+"'";
        }
        sql=sql+" ,status='"+status+"'";
        sql=sql+" where questionnaire_id="+Integer.parseInt(questionId);
        System.out.println("modify sql = "+sql);
        db.execute(sql);

        //返回状态
        response.setContentType("application/json; charset=UTF-8");
        queryBuilder.clear();
        sql=queryBuilder.getSelectStmt();
        db=new DatabaseHelper();
        rs=db.executeQuery(sql);
        processResult(rs);
        PrintWriter out = response.getWriter();
        for(int i=0;i<queryResult.length();i++)
        {
            System.out.printf("result[%d].guid=%d\n",i, queryResult.getJSONObject(i).getInt("guid"));
        }
        out.print(queryResult);
        out.flush();
        out.close();

        System.out.println("exit modifyResult");
    }


    private void getStatistics(HttpServletRequest request, HttpServletResponse response) throws JSONException, SQLException, IOException {
        System.out.println("enter AuthorizationAction.getStatistics");
        String sql = queryBuilder.getSelectStmt();
        String dateFormat = request.getParameter("interval");
        if(dateFormat==null || dateFormat.length()==0)
        {
            System.out.println("getStatistics: miss argument 'interval'");
            return;
        }
        if(Objects.equals(dateFormat, "hour"))dateFormat="%Y-%m-%d %H:00:00";
        else if(Objects.equals(dateFormat, "day"))dateFormat="%Y-%m-%d";
        else if(Objects.equals(dateFormat, "month"))dateFormat="%Y-%m";
        sql = String.format("select " +
                        "DATE_FORMAT(create_time,'%s') as tt, " +
                        " count(*) as cnt " +
                        " from (%s) as tmp " +
                        " group by tt ",
                dateFormat, sql);
        System.out.println("getStatistics sql = "+sql);
        DatabaseHelper db=new DatabaseHelper();
        ResultSet rs=db.executeQuery(sql);
        JSONArray list = new JSONArray();
        while(rs.next())
        {
            JSONObject item = new JSONObject();
            item.put("time", rs.getString("tt"));
            item.put("count", rs.getString("cnt"));
            list.put(item);
        }
        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.print(list);
        out.flush();
        out.close();
        System.out.println("exit AuthorizationAction.getStatistics");
    }


    private void processResult(ResultSet rs) throws JSONException, SQLException {
        queryResult = new JSONArray("[]");
        rs.beforeFirst();
        while(rs.next())
        {
            JSONObject item = new JSONObject();
            item.put("guid", rs.getInt("guid"));
            item.put("user_id", rs.getInt("user_id"));
            item.put("author_name", rs.getString("author_name"));
            item.put("title", rs.getString("title"));
            item.put("create_time", rs.getString("create_time"));
            item.put("change_time", rs.getString("change_time"));
            item.put("limit_time", rs.getString("limit_time"));
            item.put("user_name", rs.getString("user_name"));
            item.put("answer_num", rs.getInt("answer_num"));
            item.put("questionnaire_id", rs.getInt("questionnaire_id"));
            item.put("status", rs.getString("status"));
            item.put("answer", rs.getString("answer"));
            item.put("problem", rs.getString("problem"));
            item.put("problem_id", rs.getString("problem_id"));
            queryResult.put(item);
        }
    }
}
