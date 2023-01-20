/*******************************************************************************
* Copyright (c) 2016 IBM Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/

package com.acmeair.web;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import com.acmeair.client.CustomerClient;
import com.acmeair.securityutils.ForbiddenException;
import com.acmeair.securityutils.SecurityUtils;

import ctrlmnt.ControllableService;
import ctrlmnt.CtrlMNT;

@RestController
@RequestMapping("/")
public class AuthServiceRest extends ControllableService {

	@Value("${ms.hw}")
	private Float hw;

	private static final Logger logger = Logger.getLogger(AuthServiceRest.class.getName());

	public static final String JWT_COOKIE_NAME = "jwt_token";
	public static final String USER_COOKIE_NAME = "loggedinuser";
	private static final AtomicInteger users = new AtomicInteger(0);

	@Autowired
	private CustomerClient customerClient;

	@Autowired
	private SecurityUtils secUtils;

	@Value("${ms.name}")
	private String msname;

	public AuthServiceRest() {
		CtrlMNT mnt = new CtrlMNT(this);
		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(mnt, 0, 500, TimeUnit.MILLISECONDS);
	}

	/**
	 * Login with username/password.
	 * 
	 */
	@RequestMapping(value = "/login", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ModelAndView login(@RequestParam String login, @RequestParam String password) {
		// Test: curl -d 'login=user1' -d 'password=letmein' http://localhost:8080/login
		try {

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("attempting to login : login " + login + " password " + password);
			}

			if (!validateCustomer(login, password)) {
				throw new ForbiddenException("Invalid username or password for " + login);
			}

			// Generate simple JWT with login as the Subject
			String token = "";
			if (secUtils.secureUserCalls()) {
				token = secUtils.generateJwt(login);
			}

			// We need to pass the login and token to the CookieInterceptor so that it can
			// set response headers:
			Map<String, String> model = new HashMap<>();
			model.put("token", token);
			model.put("login", login);

			ModelAndView res = new ModelAndView(new View() {

				@Override
				public String getContentType() {
					return "text/plain";
				}

				@Override
				public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response)
						throws Exception {
					response.getWriter().print("logged in");
				}
			}, model);

			this.doWork(150l);

			return res;

		} catch (Exception e) {
			e.printStackTrace();
			throw new ForbiddenException("Error: " + e.getLocalizedMessage());
		}
	}

	@RequestMapping("/")
	public String checkStatus() {
		return "OK";
	}

	private boolean validateCustomer(String login, String password) {
		return customerClient.validateCustomer(login, password);
	}

//	private void doWork(long stime) {
//		AuthServiceRest.users.incrementAndGet();
//		ExponentialDistribution dist=new ExponentialDistribution(stime);
//		Double isTime = dist.sample();
//		Float d = (float) (isTime.floatValue() * (AuthServiceRest.users.floatValue() / this.hw));
//		try {
//			TimeUnit.MILLISECONDS.sleep(Math.max(Math.round(d), Math.round(isTime)));
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		} finally {
//			AuthServiceRest.users.decrementAndGet();
//		}
//	}

	@Override
	public Float getHw() {
		return this.hw;
	}

	@Override
	public String getName() {
		return this.msname;
	}

	@Override
	public void setHw(Float hw) {
		this.hw = hw;
	}

	@Override
	public void egress() {
		AuthServiceRest.users.decrementAndGet();
	}

	@Override
	public Integer getUser() {
		return AuthServiceRest.users.get();
	}

	@Override
	public void ingress() {
		AuthServiceRest.users.incrementAndGet();
	}
}
