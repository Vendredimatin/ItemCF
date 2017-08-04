package com;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;


public class Test {
	
	@org.junit.Test
	public void test1() throws IOException{
		ArrayList list = new MovieRecommend().readData();
		Iterator<UserContent> iterator = list.iterator();
		while(iterator.hasNext()){
			UserContent uc = iterator.next();
			System.out.println(uc.getUserId()+"\t"+uc.getMovieId()+"\t"+uc.getScore());//myeclipse输出竟然是乱序的！
		}
	}
	
	@org.junit.Test
	public void test2() throws IOException{
		MovieRecommend mr = new MovieRecommend();
		ArrayList<UserContent> list = mr.readData();
		int[][] movie = mr.movieSimilarty(list);
		mr.result(movie, "1", 10,10);//对用户1推荐，邻居为10，推荐10条
	}
}
