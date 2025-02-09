package com.myapp.app;

import static java.lang.System.in;
import static java.lang.System.out;

import java.util.Scanner;

/**
 * @author weg
 * @version 1.0
 * @description Main application
 */
public class Main {
	
	public static void main(String[] args) {
		var input = new Scanner(in);
		
		out.print("Enter a number: ");
		var number1 = input.nextDouble();
		
		out.print("Enter second number: ");
		var number2 = input.nextDouble();
		
		var product = number1 * number2;
		out.printf("The product of both numbers is: %f", product);
		
		//call method
		runOnBackgroundThread(() -> {
			out.println("Hello, world!");
		});
	}
	
	/**
	 * @param runnable runnable to run
	 * @since 1.0
	 */
	public static void runOnBackgroundThread(Runnable runnable) {
		new Thread(() -> {
			if (runnable != null) runnable.run();
		}).start();
	}
}
