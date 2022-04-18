package com.team.bf.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.team.bf.service.HeartService;
import com.team.bf.service.LikeService;
import com.team.bf.service.OpenApiService;
import com.team.bf.service.PlannerService;
import com.team.bf.service.ReviewService;
import com.team.bf.vo.PlannerLocationVO;
import com.team.bf.vo.PlannerVO;
import com.team.bf.vo.ResponseVO;

@RestController
public class MapAndPlannerController {
	private String areaCode = "39";
	@Inject
	OpenApiService openApiService;
	@Inject
	LikeService likeService;
	@Inject
	HeartService heartService;
	@Inject
	ReviewService reviewService;
	@Inject
	PlannerService plannerService;
	
	@GetMapping("/mapView")
    public ModelAndView mapView2(HttpSession session){
        String userid = (String)session.getAttribute("logId");
       ModelAndView mav = new ModelAndView();
        mav.setViewName("map/mapView");
        return mav;
    }
	
	 //마이페이지 나의 플래너 뷰
    @GetMapping("/mypage/myplan")
    public ModelAndView ModelAndView(HttpSession session) {
    	ModelAndView mav = new ModelAndView();
    	String userid = (String)session.getAttribute("logId");
    	try {
    		if(userid != null) {
    			mav.addObject("planList", plannerService.plannerSelectById(userid));
    			mav.setViewName("mypage/myplan");
    		}
    		else {
    			mav.setViewName("redirect:/login");
    		}
    		
    	}catch(Exception e) {
    		e.printStackTrace();
    		mav.setViewName("redirect:/");
    	}
    	
    	
    	return mav;
    }
    //0.여행지 정보 요청
    @PostMapping("/mapInfo")
    public String mapInfo( String pageNo,String pageCount, String searchWord, HttpSession session){
        String userid = (String)session.getAttribute("logId");
       
        System.out.println(pageNo + " " + pageCount);
        if(pageNo == null) pageNo = "1";
        if(pageCount == null) pageCount = "2";
        if(searchWord == null) searchWord = "";
        JSONArray jsonArray = openApiService.searchKeyword2(pageNo, pageCount, "all", searchWord); 
        for(int i=0; i<jsonArray.length(); i++){
           String cid = jsonArray.getJSONObject(i).get("contentid").toString();
           JSONObject Opt = openApiService.detailCommon(cid,areaCode);
           jsonArray.getJSONObject(i).put("title",Opt.get("title").toString());
           jsonArray.getJSONObject(i).put("overview",Opt.get("overview").toString());
           jsonArray.getJSONObject(i).put("firstimage",Opt.get("firstimage").toString());
           jsonArray.getJSONObject(i).put("firstimage2",Opt.get("firstimage2").toString());
           if(Opt.has("homepage"))
              jsonArray.getJSONObject(i).put("homepage", Opt.get("homepage").toString());
           else
              jsonArray.getJSONObject(i).put("homepage", "");
           jsonArray.getJSONObject(i).put("likeCount", likeService.likeSelectAll(cid));
           jsonArray.getJSONObject(i).put("heartCount", heartService.heartSelectAll(cid));
           jsonArray.getJSONObject(i).put("reviewCount", reviewService.reviewSelectByContentid(cid).size());
        
           Float avgScore = reviewService.reviewSelectAvgScore(cid);    
           if(avgScore == null)
              jsonArray.getJSONObject(i).put("avgScore", "0");
           else 
              jsonArray.getJSONObject(i).put("avgScore", String.format("%.2f",avgScore));
        }      
        //로그인 했을 경우 여행 플래너 정보
        if(userid != null) {
           //jsonArray.put("plannerList",plannerService.plannerSelectById(userid));
        }
        return jsonArray.toString();
    }
    //1.여행 계획 생성 요청
    @PostMapping("/planView")
    public ResponseEntity<HashMap<String,String>> planCreate(PlannerVO pvo, HttpSession session) {
    	String userid = (String)session.getAttribute("logId");
    	ResponseEntity<HashMap<String,String>> entity = null;
    	HashMap<String,String> result = new HashMap<String,String>();
    	
    	try {
    		
    		//작성자 등록
    		pvo.setUserid(userid);
    		result.put("status", "200");
    		//넘버링과 넘어온 contentid 수가 다를 경우
    		if(pvo.getContentidList().size() != pvo.getSeqList().size()) {
    			result.put("msg","잘못된 형식 입니다.");
    			result.put("redirect", "/planView");
    		}
    		else if(plannerService.plannerInsert(pvo) > 0){
    			//장소 정보 가공
    			List<PlannerLocationVO> pvoList = new ArrayList<PlannerLocationVO>();
    			for(int i=0; i<pvo.getSeqList().size(); i++) {
    				String contentid = pvo.getContentidList().get(i);
    				int order = pvo.getSeqList().get(i);
            		pvoList.add(new PlannerLocationVO(pvo.getNo(),contentid, order));
            	}
    			//여행 장소 생성
    			if(pvoList != null && pvoList.size() > 0)
    				plannerService.plannerLocationInsert(pvoList,pvo.getNo());
    			result.put("msg","여행 플랜 생성 성공!");
    			result.put("redirect", "/planView");
    			result.put("planner_no", Integer.toString(pvo.getNo()));
    		}
    		else {
    			//생성 실패 
    			result.put("msg","여행 플랜 생성 실패.");
    			result.put("redirect", "/planView");
    		}
    		entity = new ResponseEntity<HashMap<String,String>>(result,HttpStatus.OK);
        	
    	}catch(Exception e) {
    		//로그아웃 상태
    		e.printStackTrace();
    		result.put("msg","여행 계획 생성 에러...Error");
			result.put("status", "400");
			result.put("redirect", "/planView");
			entity = new ResponseEntity<HashMap<String,String>>(result,HttpStatus.BAD_REQUEST);
    	}
    
    	return entity;
    }
    
    //2.여행플랜 수정 요청
    @PutMapping("/planView")
    public ResponseEntity<HashMap<String,String>> planUpdate(PlannerVO pvo, HttpSession session){
    	String userid = (String)session.getAttribute("logId");
    	ResponseEntity<HashMap<String,String>> entity = null;
    	HashMap<String,String> result = new HashMap<String,String>();
    	
    	try {
    		//플랜 정보가 있을 경우
    		if(plannerService.plannerSelectByNoId(pvo.getNo(), userid) != null) {
    			//여행플랜 업데이트
    			int r = plannerService.plannerUpdate(pvo);
    			if(r>0 && pvo.getSeqList() != null && pvo.getContentidList() != null) {
    				//여행 플랜 장소 제거
    				int deleteCount = plannerService.plannerLocationDeleteByPlannerNo(pvo.getNo());
    				System.out.println("deleteCount : " + deleteCount);
    				//장소 정보 가공
        			List<PlannerLocationVO> pvoList = new ArrayList<PlannerLocationVO>();
        			for(int i=0; i<pvo.getSeqList().size(); i++) {
        				String contentid = pvo.getContentidList().get(i);
        				int seq = pvo.getSeqList().get(i);
                		pvoList.add(new PlannerLocationVO(pvo.getNo(),contentid, seq));
                	}
        			//여행 장소 생성
        			plannerService.plannerLocationInsert(pvoList,pvo.getNo());
        			result.put("status", "200");
        			result.put("msg", "여행 정보 업데이트 성공!");
        			result.put("redirect","/planView");
    			}
    		}
    		else {
    		//일치하는 여행플랜이 없을 경우 
    		//자기 플랜이 아닐 경우.
    			result.put("status", "200");
    			result.put("msg", "여행 플랜 수정 권한이 업습니다.");
    			result.put("redirect","/planView");
    		}
    		entity = new ResponseEntity<HashMap<String,String>>(result,HttpStatus.OK);
    		
    	}catch(Exception e) {
    		e.printStackTrace();
    		result.put("status", "400");
			result.put("msg", "여행플랜 업데이트 에러...Error");
			result.put("redirect","/planView");
    		entity = new ResponseEntity<HashMap<String,String>>(result,HttpStatus.BAD_REQUEST);
    	}
    	
    	return entity;
    }
    
    //3.예행 계획 삭제 요청
    @DeleteMapping("/planView")
    public ResponseEntity<HashMap<String,String>> planDelete(int no, HttpSession session){
    	String userid = (String)session.getAttribute("logId");
    	ResponseEntity<HashMap<String,String>> entity = null;
    	HashMap<String,String> result = new HashMap<String,String>();
    	
    	try {
    		PlannerVO pvo = plannerService.plannerSelectByNoId(no, userid);
    		result.put("staus", "200");
    		if(userid == null){
    			//로그인을 안함
    			result.put("msg", "로그인 후 이용해 주세요.");
    			result.put("redirect","/login");
    		}
    		else if(pvo == null) {
    			//일치하는 정보가 없음
    			result.put("msg", "일치하느 정보가 없습니다.");
    			result.put("redirect","/login");
    		}
    		else {
    			System.out.println("삭제한 데이터 : "+ plannerService.plannerDelete(no));
    			result.put("msg", "여행계획 삭제 완료!");
    			result.put("redirect","/planView");
    		}
    		entity = new ResponseEntity<HashMap<String,String>>(result,HttpStatus.OK);
    	}catch(Exception e) {
    		e.printStackTrace();
    		result.put("staus", "400");
    		result.put("msg", "여행계획 삭제 에러...Error");
    		entity = new ResponseEntity<HashMap<String,String>>(result,HttpStatus.BAD_REQUEST);
    	}
    	
    	return entity;
    }
    
    //4.여행계획 조회 
    @PostMapping("/planView/detail/{planner_no}")
    public ResponseEntity<HashMap<String,String>>  planDetailInfo(@PathVariable(value="planner_no") int no, HttpSession session){
    	String userid = (String)session.getAttribute("logId");
    	ResponseEntity<HashMap<String,String>> entity = null;
    	HashMap<String,String> result = new HashMap<String,String>();
    	
    	try {
    		PlannerVO pvo = plannerService.plannerSelectOne(no,userid);
    		result.put("status", "200");
    		if(userid == null) {	
    			result.put("msg","로그인 후 이용해 주세요.");
    			result.put("redirect","/planView");
    		}
    		else if(pvo != null) {
    			result.put("msg","일치하는 정보가 없습니다.");
    			result.put("redirect","/planView");
    		}
    		else {
        		//JSON객체 만들기
        		JSONObject jObj = new JSONObject();
        		JSONObject planObj =new JSONObject();
        		JSONArray contentList = new JSONArray();
        		JSONArray seqList = new JSONArray();
        		JSONArray memberList = new JSONArray();
        		contentList.putAll(pvo.getContentidList());
        		seqList.putAll(pvo.getContentidList());
        		memberList.putAll(plannerService.plannerMemberSelectByNo(pvo.getNo()));
        		planObj.put("no", pvo.getNo());
        		planObj.put("title", pvo.getTitle());
        		planObj.put("contendList", contentList);
        		planObj.put("seqList",seqList);
        		
        		jObj.put("pvo", planObj);
        		//데이터 삽입
        		result.put("data", jObj.toString());
        		result.put("msg","여행 플랜 불러오기 성공");
    			result.put("redirect","/planView");
    		}
    		
    		entity = new ResponseEntity<HashMap<String,String>>(result,HttpStatus.OK);
    	}catch(Exception e) {
    		e.printStackTrace();
    		result.put("status", "400");
    		result.put("msg","여행 플랜 불러오기 에러...Error");
			result.put("redirect","/planView");
    		entity = new ResponseEntity<HashMap<String,String>>(result,HttpStatus.BAD_REQUEST);
    	}
    	return entity;
    	
    }
    
    //5.여행 플랜 멤버 초대 
    @PostMapping("/planView/member")
    public ResponseEntity<HashMap<String,String>> planMemberInvite(int planner_no, String useridInvite, HttpSession session){
    	String userid = (String)session.getAttribute("logId");
    	ResponseEntity<HashMap<String,String>> entity = null;
    	HashMap<String,String> result = new HashMap<String,String>();
    	
    	try {
    		
    		result.put("status", "200");
    		PlannerVO pvo = plannerService.plannerSelectByNoId(planner_no,userid);
    		if(userid.equals(useridInvite)) {
    			result.put("msg","본인에게 초대를 보낼 수 없습니다.");
    			result.put("redirect","/planView");
    		}
    		else if(!pvo.getUserid().equals(userid)){
    			//여행 플랜을 생성한 사람만 초대할 수 있음
    			result.put("msg","초대 권한이 없습니다.");
    			result.put("redirect","/planView");
    		}
    	
    		else if(plannerService.plannerMemberSelectByNoId(planner_no, useridInvite) > 0 || pvo.getUserid().equals(useridInvite)) {
    			result.put("msg","이미 추가 되었습니다.");
    			result.put("redirect","/planView");
    		}
    		else {
    			plannerService.plannerMemberInsert(planner_no, useridInvite);
    			result.put("msg",useridInvite+" 멤버 초대 완료!");
    			result.put("redirect","/planView");
    		}
    		entity = new ResponseEntity<HashMap<String,String>>(result,HttpStatus.OK);
    	}catch(Exception e) {
    		//userid 값들이 null 일경우
    		e.printStackTrace();
    		result.put("status", "400");
    		result.put("msg","여행 플랜 멤버 초대 에러...Error");
			result.put("redirect","/planView");
    		entity = new ResponseEntity<HashMap<String,String>>(result,HttpStatus.BAD_REQUEST);
    	}
    	return entity;
		
    	
    }
    
    //6. 여행 플랜 멤버 삭제
    @DeleteMapping("/planView/member")
    public ResponseEntity<HashMap<String,String>> planMemberDelete(int no, String useridDelete, HttpSession session){
    	String userid = (String)session.getAttribute("logId");
    	ResponseEntity<HashMap<String,String>> entity = null;
    	HashMap<String,String> result = new HashMap<String,String>();
    	
    	try {
    		result.put("status", "200");
    		if(userid == null) {
    			result.put("msg", "로그인 후 이용해 주세요.");
    			result.put("redirect","/planView");
    		}
    		else {
    			if(plannerService.plannerMemberDeleteByNo(no) > 0) {
    				result.put("msg", "멤버 삭제 성공.");
    				result.put("redirect","/planView");
    			}
    			else{
    				result.put("msg", "멤버 삭제 실패.");
    				result.put("redirect","/planView");
    			}
    		}
    		
    		entity = new ResponseEntity<HashMap<String,String>>(result,HttpStatus.OK);
    	}catch(Exception e) {
    		//userid 값들이 null 일경우
    		e.printStackTrace();
    		result.put("status", "400");
    		result.put("msg","여행 플랜 멤버 초대 에러...Error");
			result.put("redirect","/planView");
    		entity = new ResponseEntity<HashMap<String,String>>(result,HttpStatus.BAD_REQUEST);
    	}
    	return entity;
		
    	
    }
    
    
    @GetMapping("/mypage/myplan")
    public ModelAndView myPlanView(){
        ModelAndView mav = new ModelAndView();
        mav.setViewName("mypage/myplan");
        return mav;
    }
}