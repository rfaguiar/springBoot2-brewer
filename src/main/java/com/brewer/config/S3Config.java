package com.brewer.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Profile("prod")
@Configuration
public class S3Config {

	@Autowired
	private Environment env;

	@Value("${brewer.amazons3.access-key}")
	private String accessKey;

	@Value("${brewer.amazons3.secret-key}")
	private String secretKey;
	
	@Bean
	public AmazonS3 amazonS3(){
		AWSCredentials credenciais = new BasicAWSCredentials(accessKey, secretKey);
		AmazonS3 amazonS3 = new AmazonS3Client(credenciais, new ClientConfiguration());
		Region regiao = Region.getRegion(Regions.US_EAST_1);
		amazonS3.setRegion(regiao);
		return amazonS3;
	}
}
