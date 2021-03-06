package com.paddavoet.bittradr.application;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.List;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import com.paddavoet.bittradr.component.integration.BitFinExAPI;
import com.paddavoet.bittradr.market.jobs.QueryMarketStateJob;
import com.paddavoet.bittradr.trader.quartz.AutowiringSpringBeanJobFactory;

/**
 * Intended to be the configuration class for the App
 * 
 * @author Riaan
 *
 */
@Configuration
public class ApplicationConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfig.class);

	public static BitFinExAPI BIT_FIN_EX_API = new BitFinExAPI();

	private static Scheduler SCHEDULER;

	private static boolean initialized;
	
	public static void initialise(ConfigurableApplicationContext appContext) {
		try {
			SCHEDULER = StdSchedulerFactory.getDefaultScheduler();

			// and start it off
			SCHEDULER.start();

			if (SCHEDULER != null) {
				initializeScheduledJobs(appContext);
				ApplicationConfig.setInitialized(true);
			} else {
				LOGGER.warn("Did not schedule the jobs, as the scheduler is null. Check logs for possible scheduler initialization errors?");
			}
		} catch (SchedulerException e) {
			LOGGER.error("Initialization error with Scheduler: ", e);
		}
	}

	public static boolean isInitialized() {
		return initialized;
	}

	public static void setInitialized(boolean initialized) {
		ApplicationConfig.initialized = initialized;
	}

	private static void initializeScheduledJobs(ConfigurableApplicationContext appContext) throws SchedulerException {

		
		AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
	    jobFactory.setApplicationContext(appContext);
	    SCHEDULER.setJobFactory(jobFactory);
	    
		// define the job and tie it to our MyJob class
		JobDetail job = newJob(QueryMarketStateJob.class).withIdentity("queryMarketState", "group1").build();

		// Tell quartz to schedule the job using our trigger
		if (!SCHEDULER.checkExists(job.getKey())) {
			LOGGER.info("Job does NOT exist for key {}, scheduling the job now.", job.getKey());

			// Trigger the job to run now, and then repeat every 60 seconds
			Trigger trigger = newTrigger().withIdentity("trigger1", "group1").forJob(job).startNow()
					.withSchedule(simpleSchedule().withIntervalInSeconds(60).repeatForever()).build();

			SCHEDULER.scheduleJob(job, trigger);
		} else {
			LOGGER.info("Job already exists for key {}, scheduling the existing job", job.getKey());

			// Trigger the job to run now, and then repeat every 60 seconds
			Trigger newTrigger = newTrigger().withIdentity("trigger_new", "group1").forJob(job).startNow()
					.withSchedule(simpleSchedule().withIntervalInSeconds(60).repeatForever()).build();

			List<? extends Trigger> oldTriggers = SCHEDULER.getTriggersOfJob(job.getKey());

			for (Trigger oldTrigger : oldTriggers) {
				SCHEDULER.rescheduleJob(oldTrigger.getKey(), newTrigger);
			}
		}
	}
}
