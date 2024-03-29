package telran.students.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import org.bson.Document;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import telran.exceptions.NotFoundException;
import telran.students.dto.IdName;
import telran.students.dto.IdNamePhone;
import telran.students.dto.Mark;
import telran.students.dto.MarksOnly;
import telran.students.dto.NameAvgScore;
import telran.students.dto.Student;
import telran.students.model.StudentDoc;
import telran.students.repo.StudentRepo;
@Service
@Slf4j
@RequiredArgsConstructor
public class StudentsServiceImpl implements StudentsService {
	private static final Object SCORE_THRESHOLD = 80;
	final StudentRepo studentRepo;
	final MongoTemplate mongoTemplate; 
	@Override
	@Transactional
	public Student addStudent(Student student) {
		long id = student.id();
		if(studentRepo.existsById(id)) {
			throw new IllegalStateException(String.format("Student %d already exists", id));
		}
		studentRepo.save(StudentDoc.of(student)); 
		log.debug("saved {}", student);
		return student;
	}

	@Override
	@Transactional
	public Student updatePhone(long id, String phone) {
		StudentDoc studentDoc = getStudent(id);
		String oldPhone = studentDoc.getPhone();
		studentDoc.setPhone(phone);
		studentRepo.save(studentDoc);
		log.debug("student {}, old phone nmber {}, new phone nunber {}", id, oldPhone, phone);
		return studentDoc.build();
	}

	private StudentDoc getStudent(long id) {
		return studentRepo.findById(id)
				.orElseThrow(() -> new NotFoundException(String.format("Student %d not found", id)));
	}

	@Override
	@Transactional
	public List<Mark> addMark(long id, Mark mark) {
		StudentDoc studentDoc = getStudent(id);
		studentDoc.addMark(mark);
		studentRepo.save(studentDoc);
		log.debug("student {}, added mark {}", id, mark);
		return studentDoc.getMarks();
	}

	@Override
	@Transactional
	public Student removeStudent(long id) {
		StudentDoc studentDoc = studentRepo.findStudentNoMarks(id);
		if(studentDoc == null) {
			throw new NotFoundException(String.format("student %d not found", id));
		}
		studentRepo.deleteById(id);
		log.debug("removed student {}, marks {} ", id, studentDoc.getMarks());
		return studentDoc.build();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Mark> getMarks(long id) {
		StudentDoc studentDoc = studentRepo.findStudentMarks(id);
		if(studentDoc == null) {
			throw new NotFoundException(String.format("student %d not found", id));
		}
		log.debug("id {}, name {}, phone {}, marks {}", 
				studentDoc.getId(), studentDoc.getName(), studentDoc.getPhone(), studentDoc.getMarks());
		return studentDoc.getMarks();
	}

	@Override
	public Student getStudentByPhone(String phoneNumber) {
		IdName studentDoc = studentRepo.findByPhone(phoneNumber);
		Student res = null;
		if(studentDoc != null) {
			res = new Student(studentDoc.getId(), studentDoc.getName(), phoneNumber);
		}
		return res;
	}

	@Override
	public List<Student> getStudentsByPhonePrefix(String phonePrefix) {
		List <IdNamePhone> students = studentRepo.findByPhoneRegex(phonePrefix + ".+");
		log.debug("number of the students having phone prefix {} is ", phonePrefix, students.size());
		return getStudents(students);
	}

	private List<Student> getStudents(List<IdNamePhone> students) {
		return students.stream().map(inp -> new Student(inp.getId(), inp.getName (), 
				inp.getPhone())).toList();
	}

	@Override
	public List<Student> getStudentsAllGoodMarks(int thresholdScore) {
		List<IdNamePhone> students = studentRepo.findByGoodMarks(thresholdScore);
		return getStudents(students);
	}

	@Override
	public List<Student> getStudentsFewMarks(int thresholdMarks) {
		List<IdNamePhone> students = studentRepo.findByFewMarks(thresholdMarks);
		return getStudents(students);	
		}

	@Override
	public List<Student> getStudentsAllGoodMarksSubject(String subject, int thresholdScore) {
		List<IdNamePhone> students = studentRepo.findByGoodMarksSubject(subject, thresholdScore);
		List<Student> res = getStudents(students);
		log.debug("Service method  getStudentsAllGoodMarksSubject OUT: {}", res );
		return res;
	}

	
	@Override
	public List<Student> getStudentsMarksAmountBetween(int min, int max) {
		List<IdNamePhone> students = studentRepo.findByAmountMarksBetween(min, max);
		return getStudents(students);
	}

	@Override
	public List<Mark> getStudentSubjectMarks(long id, String subject) {
		
		MatchOperation matchMarksSubject = Aggregation.match(Criteria.where("marks.subject").is(subject));
		
		return getStudentMarksByMatchOperation(id, matchMarksSubject);
	}
	
	private List<Mark> getStudentMarksByMatchOperation(long id, MatchOperation matchOperation) {
		if(!studentRepo.existsById(id)) {
			throw new NotFoundException(String.format("student with id %d not found", id));
		}
		MatchOperation matchStudent = Aggregation.match(Criteria.where("id").is(id));
		UnwindOperation unwindOperation = Aggregation.unwind("marks");
		ProjectionOperation projectionOperation = Aggregation.project("marks.subject", "marks.score", "marks.date");
		Aggregation pipeLine = Aggregation.newAggregation(matchStudent, unwindOperation, 
				matchOperation, projectionOperation);
		var aggregationResult = mongoTemplate.aggregate(pipeLine, StudentDoc.class, Document.class);
		List<Document> listDocuments = aggregationResult.getMappedResults();
		log.debug("listDocuments: {}", listDocuments);
		List<Mark> result = listDocuments.stream()
				.map(d -> new Mark(d.getString("subject"), 
						d.getDate("date").toInstant().atZone(ZoneId.systemDefault())
						.toLocalDate(), d.getInteger("score")))
				.toList();
		log.debug("result: {}", result);		
		return result;
	}
	
	
	@Override
	public List<NameAvgScore> getStudentAvgScoreGreater(int avgScoreThreshold) {
		UnwindOperation unwindOperation = Aggregation.unwind("marks");
		GroupOperation groupOperation = Aggregation.group("name").avg("marks.score").as("avgMark");
		MatchOperation matchOperation = Aggregation.match(Criteria.where("avgMark").gt(avgScoreThreshold));
		SortOperation sortOperation = Aggregation.sort(Direction.DESC, "avgMark");
		Aggregation pipeLine = Aggregation.newAggregation(unwindOperation, groupOperation, 
				matchOperation, sortOperation);
		List<NameAvgScore> res = mongoTemplate.aggregate(pipeLine, StudentDoc.class, Document.class)
				.getMappedResults().stream().map(d -> new NameAvgScore(d.getString("_id"), 
						d.getDouble("avgMark").intValue())).toList();
		
		log.debug("result: {}", res);
		return res;
	}
/********************************HW #75*********************************************/
	@Override
	public List<Mark> getStudentMarksAtDates(long id, LocalDate from, LocalDate to) {
		
		MatchOperation matchMarksDate = Aggregation.match(Criteria.where("marks.date").gte(from)
				.lte(to));
		
		return getStudentMarksByMatchOperation(id, matchMarksDate);
	}

	@Override
	public List<String> getBestStudents(int nStudents) {
		// TODO 
		//returns list of a given number of the best students
		//Best students are the ones who have most scores greater than 80
		UnwindOperation  unwindOperation = Aggregation.unwind("marks");
		MatchOperation matchBestMarks = Aggregation.match(Criteria.where("marks.score").gt(SCORE_THRESHOLD));
		GroupOperation groupOperation = Aggregation.group("name").count().as("nBestMarks");
		SortOperation sortOperation = Aggregation.sort(Direction.DESC, "nBestMarks");
		ProjectionOperation projectionOperation = Aggregation.project("name");
		LimitOperation limitNStudents = Aggregation.limit(nStudents);
		Aggregation pipeLine = Aggregation.newAggregation(unwindOperation, matchBestMarks, groupOperation, 
				sortOperation, projectionOperation, limitNStudents);
		var aggregetionResult = mongoTemplate.aggregate(pipeLine, StudentDoc.class, Document.class);
		List<Document> listDocuments = aggregetionResult.getMappedResults();
		log.debug("list Documents: {}", listDocuments);
		List<String> result = listDocuments.stream().map(d -> d.getString("_id")).toList();
		log.debug("result: {}", result);
		return result;
	}

	@Override
	public List<String> getWorstStudents(int nStudents) {
		// TODO 
		//returns list of a given number of the worst students
		//Worst students are the ones who have least sum's of all scores
		//Students who have no scores at all should be considered as worst
		//instead of GroupOperation to apply AggregationExpression (with AccumulatorOperators.Sum) and ProjectionOperation for adding new fields with computed values 
		//AggregationExpression homeworkExpression = AccumulatorOperators.Sum.sumOf("homework");
		AggregationExpression scoreExpression = AccumulatorOperators.Sum.sumOf("marks.score");
		ProjectionOperation projectionOperation = Aggregation.project("name")
				.and(scoreExpression).as("sumScores");
		SortOperation sortOperation = Aggregation.sort(Direction.ASC, "sumScores");
		LimitOperation limitOperation = Aggregation.limit(nStudents);
		ProjectionOperation projectFinal = Aggregation.project("name");

		Aggregation pipeLine = Aggregation.newAggregation(projectionOperation, sortOperation, 
				limitOperation, projectFinal);
		var aggregationResult = mongoTemplate.aggregate(pipeLine, StudentDoc.class, Document.class);
		List<Document> listDocuments = aggregationResult.getMappedResults();
		log.debug("list documents: {}",  listDocuments);
		List<String> result = listDocuments.stream().map(d -> d.getString("name")).toList();
		log.debug("result: {}", result);
		return result;
		
	}

	

}
