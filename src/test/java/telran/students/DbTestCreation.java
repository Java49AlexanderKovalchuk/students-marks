package telran.students;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.assertj.core.util.Arrays;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import telran.students.repo.StudentRepo;
import telran.students.dto.*;
import telran.students.model.*;

@Component 
@RequiredArgsConstructor
public class DbTestCreation {
	final StudentRepo studentRepo;
	final static long ID_1 = 1l;
	final static String NAME_1 = "name1";
	final static String PONE_1 = "051-12345667";
	final static long ID_2 = 2l;
	final static String NAME_2 = "name2";
	final static String PONE_2 = "052-12345667";
	final static long ID_3 = 3l;
	final static String NAME_3 = "name3";
	final static String PONE_3 = "053-12345667";
	final static long ID_4 = 4l;
	final static String NAME_4 = "name4";
	final static String PONE_4 = "054-12345667";
	final static long ID_5 = 5l;
	final static String NAME_5 = "name5";
	final static String PONE_5 = "055-12345667";
	final static long ID_6 = 6l;
	final static String NAME_6 = "name6";
	final static String PONE_6 = "056-12345667";
	final static long ID_7 = 7l;
	final static String NAME_7 = "name7";
	final static String PONE_7 = "057-12345667";
	//for test addStudent:
	final static long ID_8 = 8l;
	final static String NAME_8 = "name8";
	final static String PHONE_8 = "058-1234567";
	//for test updatePhone:
	final String PHONE_UPDATED = "051-1111111";
	//for test addMark:
	final Mark addedMark = new Mark(SUBJECT_1, DATE_1, 100);
	//for test removeStudent:
	final Student studentRemoving = new Student(ID_1, NAME_1, PONE_1);
	
	final static String SUBJECT_1 = "subject1";
	final static String SUBJECT_2 = "subject2";
	final static String SUBJECT_3 = "subject3";
	final static String SUBJECT_4 = "subject4";
	private static final LocalDate DATE_1 = LocalDate.parse("2023-10-20");
	private static final LocalDate DATE_2 = LocalDate.parse("2023-11-20");
	private static final LocalDate DATE_3 = LocalDate.parse("2023-12-20");
	private static final LocalDate DATE_4 = LocalDate.parse("2024-01-01");
	Student[] students = {
		new Student(ID_1, NAME_1, PONE_1), 
		new Student(ID_2, NAME_2, PONE_2),
		new Student(ID_3, NAME_3, PONE_3),
		new Student(ID_4, NAME_4, PONE_4),
		new Student(ID_5, NAME_5, PONE_5),
		new Student(ID_6, NAME_6, PONE_6),
		new Student(ID_7, NAME_7, PONE_7),
		
	};
	
	Student addedStudent = new Student(ID_8, NAME_8, PHONE_8);
	Student studentUdatedPhone = new Student(ID_1, NAME_1, PHONE_UPDATED);
	
	Mark[][] marks = {
			{
				new Mark(SUBJECT_1, DATE_1, 80),
				new Mark(SUBJECT_1, DATE_2, 90),
				new Mark(SUBJECT_2, DATE_2, 70),
			}, 
			{
				new Mark(SUBJECT_3, DATE_1, 70),
			},
			{
				new Mark(SUBJECT_1, DATE_1, 80),
				new Mark(SUBJECT_4, DATE_3, 70),
			},
			{
				new Mark(SUBJECT_2, DATE_1, 100),
				new Mark(SUBJECT_3, DATE_3, 90),
				new Mark(SUBJECT_4, DATE_4, 90),
			
			}, 
			{
				new Mark(SUBJECT_2, DATE_1, 70),
				new Mark(SUBJECT_3, DATE_3, 70),
			},
			{
				new Mark(SUBJECT_1, DATE_1, 100),
				new Mark(SUBJECT_2, DATE_2, 100),
				new Mark(SUBJECT_3, DATE_3, 100),
				new Mark(SUBJECT_4, DATE_4, 100),
			}, 
			
			{}
	};
	public void createDb() {
		studentRepo.deleteAll();
		List<StudentDoc> studentDocs = IntStream.range(0, students.length).mapToObj(this::indexToStudent).toList();
		studentRepo.saveAll(studentDocs);
		
	}
	public Mark[] getStudentMarks(long id) {
		return marks[(int) (id - 1)];
	}
	StudentDoc indexToStudent(int index) { 
		StudentDoc res = StudentDoc.of(students[index]);
		for(Mark mark : marks[index]) {
			res.addMark(mark);
		}
		return res;
	}
	
	public List<Student> getAllStudents() {
		return studentRepo.findAll().stream().map(s -> s.build()).toList();
	}
	
	public Student getStudentUpdatedPhone(long id) {
		return studentRepo.findStudentNoMarks(id).build();
	}
	public List<Mark> marksWithAdded(long id, Mark addedMark) {
		Mark[] studentMarks = getStudentMarks(id);
		List listStudentMarks = Arrays.asList(studentMarks);
		listStudentMarks.add(addedMark);
		return listStudentMarks;
	}
	public List<Mark> getStudentMarksActual(long id) {
		return studentRepo.findStudentMarks(id).getMarks();
	}
	

}
