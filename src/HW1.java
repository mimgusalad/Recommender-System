import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

class CompareDoubleThenName implements Comparator<Rating> {
    @Override
    public int compare(Rating o1, Rating o2) {
        int cmp_score = Double.compare(o2.score, o1.score);
        if (cmp_score == 0) {
            String alpha1 = o1.content.substring(0, 1);
            int num1 = Integer.parseInt(o1.content.substring(1));

            String alpha2 = o2.content.substring(0, 1);
            int num2 = Integer.parseInt(o2.content.substring(1));

            int cmp = alpha1.compareTo(alpha2);
            if (cmp == 0) {
                return Integer.compare(num1, num2);
            } else {
                return cmp;
            }
        }
        return cmp_score;
    }
}

class Rating implements Comparable<Rating> {
    String content;
    double score;

    public Rating(String content, double score) {
        this.content = content;
        this.score = score;
    }

    @Override
    public int compareTo(Rating o) {
        String alpha1 = this.content.substring(0, 1);
        int num1 = Integer.parseInt(this.content.substring(1));

        String alpha2 = o.content.substring(0, 1);
        int num2 = Integer.parseInt(o.content.substring(1));

        int cmp = alpha1.compareTo(alpha2);
        if (cmp == 0) {
            return Integer.compare(num1, num2);
        } else {
            return cmp;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, score);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Rating other = (Rating) obj;
        return Objects.equals(content, other.content)
                && Double.doubleToLongBits(score) == Double.doubleToLongBits(other.score);
    }

    @Override
    public String toString() {
        String roundoff = String.format("%.3f", score);
        return "(" + content + ", " + roundoff + ")";
    }

}

class Similarity implements Comparable<Similarity> {
    int user;
    double sim;

    public Similarity(int user, double similarity) {
        this.user = user;
        this.sim = similarity;
    }

    @Override
    public int compareTo(Similarity o) {
      return Double.compare(o.sim, this.sim);
    }

   
    @Override
    public int hashCode() {
        return Objects.hash(sim, user);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Similarity other = (Similarity) obj;
        return Double.doubleToLongBits(sim) == Double.doubleToLongBits(other.sim) && user == other.user;
    }

    @Override
    public String toString() {
        String roundoff = String.format("%.6f", sim);
        return "\t사용자 id:   " + user + ",  유사도: " + roundoff;
    }

}

public class HW1 {

    public static void main(String[] args) throws FileNotFoundException {
        String filename;
        int target;
        int n;
        int k;
        int num_user;

        System.out.println("파일 이름, target 사용자, 참고인 수, 항목 수?");
        Scanner sc = new Scanner(System.in);
        filename = sc.next();
        target = sc.nextInt();
        n = sc.nextInt();
        k = sc.nextInt();
        
        Scanner sc1 = new Scanner(new File(filename));
        num_user = sc1.nextInt();
        HashMap<Integer, Set<Rating>> rating_map = new HashMap<>(num_user);

        while (sc1.hasNext()) {
            int user;
            String content;
            Double score;

            user = sc1.nextInt();
            content = sc1.next();
            score = sc1.nextDouble();

            if (!rating_map.containsKey(user)) {
                Set<Rating> rating = new HashSet<>();
                rating.add(new Rating(content, score));
                rating_map.put(user, rating);

            } else {
                Set<Rating> rating_set = rating_map.get(user);
                Iterator<Rating> iterator = rating_set.iterator();
                while(iterator.hasNext()){
                    Rating rating = iterator.next();
                    if(rating.content.equals(content)){
                        iterator.remove();
                    }
                }
                rating_set.add(new Rating(content, score));
            }
        }

        for (Map.Entry<Integer, Set<Rating>> entry : rating_map.entrySet()) {
            double total = 0;
            double size = entry.getValue().size();

            for (Rating value : entry.getValue()) {
                total += value.score;
            }
            for (Rating value : entry.getValue()) {
                value.score -= total / size;
            }
        }

        System.out.println("1. 사용자 " + target + "의 콘텐츠와 정규화 점수:");

        SortedSet<Rating> normalized_target = new TreeSet<>(rating_map.get(target));
        System.out.println("\t"+normalized_target);

        Set<String> target_contents = new HashSet<>();
        double target_size = 0;
        for (Rating rating : rating_map.get(target)) {
            target_size += Math.pow(rating.score, 2);
            target_contents.add(rating.content);
        }

        PriorityQueue<Similarity> similarity_map = new PriorityQueue<>(num_user);

        for (Map.Entry<Integer, Set<Rating>> entry : rating_map.entrySet()) {
            double sample_size = 0;
            double common_size = 0;

            Set<String> common_contents = new HashSet<>();
            for (Rating value : entry.getValue()) {
                common_contents.add(value.content);
                if (!entry.getKey().equals(target)) {
                    sample_size += Math.pow(value.score, 2);
                }
            }
            common_contents.retainAll(target_contents);

            for (String content : common_contents) {
                for (Rating sample_user : entry.getValue()) {
                    for (Rating target_user : normalized_target) {
                        if (sample_user.content.equals(content) && target_user.content.equals(content)) {
                            common_size += sample_user.score * target_user.score;
                        }
                    }
                }

            }
            double similarity = common_size / (Math.sqrt(target_size) * Math.sqrt(sample_size));
            if (Double.isFinite(similarity)) {
                similarity_map.add(new Similarity(entry.getKey(), similarity));
            }
        }

        System.out.println("\n2. 유사한 사용자 id와 유사도 리스트");

        HashMap<String, Double> candidate_contents = new HashMap<>();
        for (int i = 0; i < n; i++) {
            Similarity sample = similarity_map.remove();
            System.out.println(sample);
            
            Set<String> uncommon_contents = new HashSet<>();
           
            for(Rating rating : rating_map.get(sample.user)) {
            	uncommon_contents.add(rating.content);
            }
            
            uncommon_contents.removeAll(target_contents);

            for(Rating rating : rating_map.get(sample.user)){
                for(String content : uncommon_contents){
                    if(rating.content.equals(content)){
                        double weighted = sample.sim * rating.score;
                        candidate_contents.put(content, candidate_contents.getOrDefault(content, 0.0)+weighted);
                    }
                }
            }

        }

        PriorityQueue<Rating> chosen_contents = new PriorityQueue<>(candidate_contents.size(), new CompareDoubleThenName());
        for(Map.Entry<String, Double> entry : candidate_contents.entrySet()){
            String content = entry.getKey();
            Double score = entry.getValue();
            Rating rating = new Rating(content,score);
            chosen_contents.add(rating);
        }

        System.out.println("\n3. 사용자 "+target+"에게 추천할 콘텐츠와 추천 점수");

        SortedSet<Rating> recommendations = new TreeSet<>(new CompareDoubleThenName());

        try{
        	for(int i=0; i<k; i++) {
        		recommendations.add(chosen_contents.remove());
        	}
        }catch(NoSuchElementException e) {
        }
        
        System.out.println("\t"+recommendations);
        
        sc.close();
        sc1.close();
    }

}
