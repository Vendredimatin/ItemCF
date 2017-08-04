package com;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

/**
 * ItemCF 基于物品的协同过滤算法
 * 和用户历史上感兴趣的物品越相似的物品，越有可能在用户的推荐列表中获得比较高的排名
 * 格式化输入——>获取用户物品倒排表——>获取每个电影的评论用户数——>计算相似度矩阵——>预测评分——>推荐结果
 */
@SuppressWarnings("resource")
public class MovieRecommend 
{
	private static Collection<UserContent> list=new ArrayList<UserContent>();
	private static Map<String,Integer> movieMap=new LinkedHashMap<String,Integer>();//统计电影总数
	private static Map<String,ArrayList<String>> userMap=new LinkedHashMap<String,ArrayList<String>>();//存放用户-评过分的电影
	private static Map<String,Integer> movieUserMap=new LinkedHashMap<String,Integer>();//存放每个电影的用户数量
	private static Map<String,Double> similaryMap=new LinkedHashMap<String, Double>();//存放相似度
	private static Map<String,Double> resultMap=new LinkedHashMap<String, Double>();//存放推荐结果
	
	/**
	 * 对数据预处理，将评分信息存入集合
	 */
	public ArrayList<UserContent> readData() throws IOException{
		BufferedReader read=new BufferedReader(new FileReader("C:/Users/zzd/Desktop/ml-100k/u.data"));
		String str;
		while((str=read.readLine())!=null){
			String[] split = str.split("\t");//只获取文件中的前三个数据
			UserContent uc=new UserContent();
			uc.setUserId(split[0]);
			uc.setMovieId(split[1]);
			uc.setScore(split[2]);
			list.add(uc);
		}
		return (ArrayList<UserContent>)list;
	}
	
	/**
	 * 计算电影间的相似度矩阵
	 */
	public int[][] movieSimilarty(ArrayList<UserContent> list){
		/**
		 * 获取用户-电影倒排表
		 */
		for(int i=0;i<list.size();i++){//map中不允许存放重复数据，利用这个特点计算出共有多少部电影
			UserContent uc=(UserContent) list.get(i);
			String movieId = uc.getMovieId();
			movieMap.put(movieId, 1);
			
			String userId = uc.getUserId();
			if(userMap.containsKey(userId)){//集合中存在userId
				ArrayList<String> movieList=userMap.get(userId);;//获取之前的list 每个用户评过分的电影
				if(movieList!=null&&!movieList.contains(movieId)){//电影不存在
					movieList.add(movieId);
					userMap.remove(userId);//乱序的
					userMap.put(userId, movieList);
				}
			}else{//集合中不存在userId
				ArrayList<String> movieList1=new ArrayList<String>();
				movieList1.add(movieId);
				userMap.put(userId, movieList1);
			}
		}
		
		/***
		 * 统计每个电影被多少用户评分过
		 */
		int movieCount=movieMap.size();//电影总数
		int movie[][]=new int[movieCount][movieCount];//相似度矩阵
		
		for(Entry<String, ArrayList<String>> e:userMap.entrySet()){
			ArrayList<String> movieList = (ArrayList<String>)e.getValue();
			for(int i=0;i<movieList.size();i++){
				String movieId = movieList.get(i);
				if(movieUserMap.containsKey(movieId)){//如果存在
					int count=movieUserMap.get(movieId).intValue()+1;
					movieUserMap.remove(movieId);
					movieUserMap.put(movieId, count);
				}else{//不存在
					movieUserMap.put(movieId, 1);
				}
			}
		}
		
		/***
		 * 构建矩阵
		 */
		
		/*不用排序了，因为数据中电影id是从0-1682连续的，若是不连续的要排序
		//获取所有movieId，并排序,与二维数组的下标对应
		int movieId[]=new int[movieCount];
		int i=0;
		for(Entry<String, Integer> e:movieMap.entrySet()){
			movieId[i++]=Integer.parseInt(e.getKey());
		}
		Arrays.sort(movieId);//排序
		for (int j : movieId) {
			System.out.println(j);
		}*/
		
		for(Entry<String,ArrayList<String>> e:userMap.entrySet()){//遍历建立矩阵
			ArrayList<String> movieList = e.getValue();
			int movieId[]=new int[movieCount];//存放电影Id
			int n=0;
			for(int i=0;i<movieList.size();i++){//将电影Id放到数组里
				movieId[n++]=Integer.parseInt(movieList.get(i));
			}
			//对同一个用户评分的任意两个电影，矩阵中都+1
			if(movieId.length>1){//至少要有两个电影
				for(int i=0;i<movieId.length-1;i++){
					for(int j=i+1;j<movieId.length;j++){
						if(movieId[i]!=0&&movieId[j]!=0){
							movie[movieId[i]-1][movieId[j]-1]+=1;
							movie[movieId[j]-1][movieId[i]-1]+=1;
						}
					}
				}
			}
		}
		
		/**
		 * 测试
		 */
		/*System.out.println("电影总数："+movieCount); //test1
		
		System.out.println("每个用户评过分的电影");
		for(Entry<String, ArrayList<String>> e:userMap.entrySet()){  //test2
			System.out.print(e.getKey()+":\t[");
			ArrayList<String> list2 = (ArrayList<String>)e.getValue();
			for(int i=0;i<list2.size();i++){
				System.out.print(list2.get(i)+",");
			}
			System.out.println("]");
		}
		
		System.out.println("每个电影的用户数");
		for(Entry<String,Integer> e:movieUserMap.entrySet()){  //test3
			System.out.println(e.getKey()+"： "+e.getValue()+"人");
		}
		
		System.out.println("所有电影：");//test4
		for(Entry e:movieMap.entrySet()){
			System.out.print(e.getKey()+" ");
		}
		System.out.println();
		
		System.out.println("相似度矩阵");
		for(int i=0;i<movie.length;i++){//test5
			for(int j=0;j<movie.length;j++){
				System.out.print(movie[i][j]+" ");
			}
			System.out.println();
		}
		*/
		return movie;
	}
	
	/**
	 * 计算推荐结果
	 * 对一个用户推荐，选取k个相似度最接近的，排序后推荐前n个
	 */
	public void result(int movie[][],String userId,int k,int n){
		/**
		 *预测用户对其他未评过分的电影的喜爱程度
		 *遍历电影集合
		 */
		
		for(Entry<String ,Integer> e:movieMap.entrySet()){
			String movieId=e.getKey();
			ArrayList<String> list = userMap.get(userId);//用户评过分的电影集合
			if(list.contains(movieId)){
				continue;
			}else{//对未评过分的电影预测评分
				//计算相似度
//				System.out.println("推荐电影："+movieId);
				for(int i=0;i<movieMap.size();i++){
					if(list.contains(String.valueOf(i+1))){//该用户必须包含被推荐电影的相似电影
						int sum=movie[i][Integer.parseInt(movieId)-1];
						int count=movieUserMap.get(movieId);
						int count1=movieUserMap.get(String.valueOf(i+1));
						if(sum==0||count==0||count1==0){
							continue;
						}
						double similary=((double)sum)/Math.sqrt((double)(count*count1));//最终相似度
						similaryMap.put(String.valueOf(i+1), similary);
//						System.out.println(movieId+"和电影"+(i+1)+"的相似度为："+similary);
					}
				}
				
				//排序 选取k个和该电影相似度最高的(因为相似度不高的参与运算会不准确!)
				//用二维数组，对value排序，第一行放相似度，第二行放与之对应的电影id
				double similarySort[][]=new double[2][movieMap.size()]; 
				int i=0;
				for(Entry<String,Double> e1:similaryMap.entrySet()){//存进数组
					double similary=e1.getValue();
					int movieId1=Integer.parseInt(e1.getKey());
					similarySort[0][i]=similary;
					similarySort[1][i++]=movieId1;
				}
				for(int i1=0;i1<movieMap.size()-1;i1++){//对相似度排序，电影id也跟着交换
					for(int j1=i1+1;j1<movieMap.size();j1++){
						if(similarySort[0][i1]<similarySort[0][j1]){
							double t;
							t=similarySort[0][i1];
							similarySort[0][i1]=similarySort[0][j1];
							similarySort[0][j1]=t;
							int t1;
							t1=(int) similarySort[1][i1];
							similarySort[1][i1]=similarySort[1][j1];
							similarySort[1][j1]=t1;
						}
					}
				}
				
				
				//计算用户对某电影的评分
				double recommend=0;
				//从排序后的数组中取前k个最接近的相似度，计算
				for(int m=0;m<k;m++){
					String movieId1=String.valueOf((int)similarySort[1][m]);
					Double similary=similarySort[0][m];
					Iterator<UserContent> iterator = this.list.iterator();
					while(iterator.hasNext()){//找到用户的评分
						UserContent uc = iterator.next();
						if(uc.getMovieId().equals(movieId1)){
							recommend+=(similary*Integer.parseInt(uc.getScore()));
							break;//list中有重复的用户id
						}
				}
			}
//				System.out.println("对电影"+movieId+"的评分为："+recommend);
				
				/*对所有的电影进行计算，而不是前k个
				 for(Entry<String,Double> e1:similaryMap.entrySet()){
					String movieId1=e1.getKey();
						Double similary = e1.getValue();
						//相似度*用户对另一个的评分 
						Iterator<UserContent> iterator = this.list.iterator();
						while(iterator.hasNext()){//找到用户的评分
							UserContent uc = iterator.next();
							if(uc.getMovieId().equals(movieId1)){
								recommend+=(similary*Integer.parseInt(uc.getScore()));
								break;//list中有重复的用户id
							}
						}
				}
				*/
				
//				System.out.println(recommend);
				similaryMap.clear();//将集合清除！！！
				resultMap.put(movieId, recommend);//将结果放到集合中
			}
		}
		//推荐结果取前n个
		double resultSort[][]=new double[2][resultMap.size()]; 
		int j=0;
		for(Entry<String,Double> e1:resultMap.entrySet()){//存进数组
			double recommend1=e1.getValue();
			int movieId1=Integer.parseInt(e1.getKey());
			resultSort[0][j]=recommend1;
			resultSort[1][j++]=movieId1;
		}
		for(int i1=0;i1<resultMap.size()-1;i1++){//对相似度排序，电影id也跟着交换
			for(int j1=i1+1;j1<resultMap.size();j1++){
				if(resultSort[0][i1]<resultSort[0][j1]){
					double t;
					t=resultSort[0][i1];
					resultSort[0][i1]=resultSort[0][j1];
					resultSort[0][j1]=t;
					int t1;
					t1=(int) resultSort[1][i1];
					resultSort[1][i1]=resultSort[1][j1];
					resultSort[1][j1]=t1;
				}
			}
		}
		//输出结果
		System.out.println("推荐结果:"+"取前"+n+"条");
		for(int m=0;m<n;m++){
			System.out.println("电影id:"+(int)resultSort[1][m]+"   推荐指数："+resultSort[0][m]);
		}
		
	}
	
}

