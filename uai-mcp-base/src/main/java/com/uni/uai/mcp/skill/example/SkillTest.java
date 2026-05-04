package com.uni.uai.mcp.skill.example;
import dev.langchain4j.skills.FileSystemSkill;
import dev.langchain4j.skills.FileSystemSkillLoader;
import dev.langchain4j.skills.Skills;
import dev.langchain4j.skills.shell.ShellSkills;

import java.nio.file.Path;
import java.util.List;

public class SkillTest {

	public static void main(String[] args) {
		List<FileSystemSkill> allSkills = FileSystemSkillLoader.loadSkills(Path.of("/Users/pengjian/work/skills/"));
		FileSystemSkill oneSkill = FileSystemSkillLoader.loadSkill(Path.of("/Users/pengjian/work/skills/after-sales")); 
		System.out.println("skills count = " + allSkills.size());
		System.out.println("single skill = " + oneSkill.name());
		
		Skills skills = Skills.from(allSkills);
		System.out.println(skills.formatAvailableSkills());
		
		ShellSkills shellSkills = ShellSkills.from(allSkills);
		System.out.println(shellSkills.formatAvailableSkills());



	}

}
