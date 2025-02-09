package weg.ide.tools.java2smali.ui.services;

import static javax.tools.StandardLocation.CLASS_OUTPUT;
import static javax.tools.StandardLocation.CLASS_PATH;
import static javax.tools.StandardLocation.PLATFORM_CLASS_PATH;
import static weg.apkide.smali.smali.SmaliParser.*;

import com.android.tools.r8.CompilationMode;
import com.android.tools.r8.D8;
import com.android.tools.r8.D8Command;
import com.android.tools.r8.OutputMode;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;
import org.eclipse.jdt.internal.compiler.batch.FileSystem;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipFile;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

import weg.apkide.smali.baksmali.Baksmali;
import weg.apkide.smali.baksmali.BaksmaliOptions;
import weg.apkide.smali.dexlib2.DexFileFactory;
import weg.apkide.smali.dexlib2.Opcodes;
import weg.apkide.smali.dexlib2.iface.DexFile;
import weg.apkide.smali.org.antlr.runtime.CommonTokenStream;
import weg.apkide.smali.org.antlr.runtime.Token;
import weg.apkide.smali.org.antlr.runtime.tree.CommonTree;
import weg.apkide.smali.org.antlr.runtime.tree.TreeVisitor;
import weg.apkide.smali.org.antlr.runtime.tree.TreeVisitorAction;
import weg.apkide.smali.smali.SmaliFlexLexer;
import weg.apkide.smali.smali.SmaliParser;
import weg.ide.tools.java2smali.common.AppLog;
import weg.ide.tools.java2smali.common.HighlightSpace;
import weg.ide.tools.java2smali.common.IoUtils;
import weg.ide.tools.java2smali.ui.App;
import weg.ide.tools.java2smali.ui.HighlightModel;
import weg.ide.tools.java2smali.ui.services.java.JavaLexer;

public class CodeService {
	public static final int PlainStyle = 0;
	public static final int KeywordStyle = 1;
	public static final int OperatorStyle = 2;
	public static final int SeparatorStyle = 3;
	public static final int StringStyle = 4;
	public static final int NumberStyle = 5;
	public static final int MetadataStyle = 6;
	public static final int IdentifierStyle = 7;
	public static final int NamespaceStyle = 8;
	public static final int TypeStyle = 9;
	public static final int FieldStyle = 10;
	public static final int VariableStyle = 11;
	public static final int FunctionStyle = 12;
	public static final int FunctionCallStyle = 13;
	public static final int ParameterStyle = 14;
	public static final int CommentStyle = 15;
	public static final int DocCommentStyle = 16;
	
	private final Object myLock = new Object();
	private boolean myShutdown;
	
	private boolean myCompile;
	private CompilerCallback myCompilerCallback;
	
	private boolean myHighlight;
	private boolean myHighlightJava;
	private boolean myHighlightSmali;
	private HighlightModel myJavaModel;
	private HighlightModel mySmaliModel;
	private final JavaLexer myJavaLexer = new JavaLexer(11);
	private final SmaliFlexLexer mySmaliLexer = new SmaliFlexLexer(28);
	private Compiler myCompiler;
	private final SmaliParser myParser = new SmaliParser(null);
	private final HighlightSpace myHighlightSpace = new HighlightSpace(10000);
	private final HighlightSpace mySyntaxHighlightSpace = new HighlightSpace(10000);
	private ErrorListener myErrorListener;
	
	public CodeService() {
		Thread thread = new Thread(null, () -> {
			try {
				while (!myShutdown) {
					synchronized (myLock) {
						if (!myShutdown) {
							
							synchronized (myLock) {
								if (myCompile) {
									compile();
									myCompile = false;
								}
							}
							
							synchronized (myLock) {
								if (myHighlight) {
									if (myHighlightJava) {
										highlightJava();
										myHighlightJava = false;
									}else if (myHighlightSmali) {
										highlightSmali();
										myHighlightSmali = false;
									}
									
									myHighlight = false;
								}
							}
						}
						myLock.wait(50L);
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}, "CodeEngine", 8 * 1024 * 1024);
		thread.setPriority(2);
		thread.start();
	}
	
	private void compile() {
		try {
			File androidJar = new File(App.getContext().getFilesDir(), "android.jar");
			File coreLambdaJar = new File(App.getContext().getFilesDir(), "core-lambda-stubs.jar");
			if (!androidJar.exists()) {
				InputStream inputStream = App.getContext().getAssets().open("android.jar");
				FileOutputStream outputStream = new FileOutputStream(androidJar);
				try {
					IoUtils.copy(inputStream, outputStream);
				} finally {
					IoUtils.safeClose(inputStream, outputStream);
				}
			}
			if (!coreLambdaJar.exists()) {
				InputStream inputStream = App.getContext().getAssets().open("core-lambda-stubs.jar");
				FileOutputStream outputStream = new FileOutputStream(coreLambdaJar);
				try {
					IoUtils.copy(inputStream, outputStream);
				} finally {
					IoUtils.safeClose(inputStream, outputStream);
				}
			}
			
			File file = new File(App.getContext().getFilesDir().getAbsolutePath() + "/project/Main.java");
			
			if (file.exists())
				file.delete();
			
			file.getParentFile().mkdirs();
			
			file.createNewFile();
			
			try (FileWriter writer = new FileWriter(file)) {
				writer.write(myJavaModel.getText());
			}
			
			long time = System.currentTimeMillis();
			File outPath = new File(App.getContext().getFilesDir().getAbsolutePath() + "/project/java-out/" + time);
			outPath.mkdirs();
			
			internalCompile(androidJar, coreLambdaJar, file, "11", "11", outPath.getAbsolutePath());
			
			List<File> outClasses = new ArrayList<>();
			findFile(outPath, ".class", outClasses);
			
			File outDexZip = new File(App.getContext().getFilesDir().getAbsolutePath() + "/project/dex-out/" + time + "/main.dex.zip");
			File outDex = new File(App.getContext().getFilesDir().getAbsolutePath() + "/project/dex-out/" + time);
			if (outDexZip.exists()) {
				outDexZip.delete();
			}
			outDexZip.getParentFile().mkdirs();
			var builder = D8Command.builder();
			builder.addClasspathFiles(androidJar.toPath(), coreLambdaJar.toPath());
			builder.setIntermediate(false);
			builder.setOutput(outDexZip.toPath(), OutputMode.DexIndexed);
			var classesPaths = new ArrayList<Path>();
			for (File outClass : outClasses) {
				classesPaths.add(outClass.toPath());
			}
			builder.addProgramFiles(classesPaths);
			builder.setMode(CompilationMode.DEBUG);
			D8.run(builder.build());
			
			ZipFile zipFile = new ZipFile(outDexZip);
			var entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				var element = entries.nextElement();
				if ((!element.isDirectory()) && element.getName().endsWith(".dex")) {
					var input = zipFile.getInputStream(element);
					var outFile = new File(outDex, element.getName());
					IoUtils.copy(input, new FileOutputStream(outFile));
				}
			}
			IoUtils.safeClose(zipFile);
			
			var dexList = new ArrayList<File>();
			if (outDex.exists()) {
				var list = outDex.listFiles();
				if (list != null) {
					for (File f : list) {
						if (f.getName().endsWith(".dex"))
							dexList.add(f);
					}
				}
			}
			
			File smaliOut = new File(App.getContext().getFilesDir().getAbsolutePath() + "/project/smali-out/" + time);
			smaliOut.mkdirs();
			BaksmaliOptions options = new BaksmaliOptions();
			
			for (File dexFile : dexList) {
				DexFile dex = DexFileFactory.loadDexFile(dexFile, Opcodes.getDefault());
				Baksmali.disassembleDexFile(dex, new File(smaliOut, dexFile.getName()), 6, options);
			}
			
			var smaliFiles = new ArrayList<File>();
			var smaliList = new ArrayList<String>();
			findFile(smaliOut, ".smali", smaliFiles);
			
			for (File smaliFile : smaliFiles) {
				smaliList.add(smaliFile.getAbsolutePath());
			}
			
			myCompilerCallback.compileSuccessful(smaliList);
			
		} catch (Exception e) {
			e.printStackTrace();
			myCompilerCallback.compileFailure(e.getLocalizedMessage());
		}
		
		
	}
	
	public void setErrorListener(ErrorListener errorListener) {
		myErrorListener = errorListener;
	}
	
	private void highlightJava() {
		String text = myJavaModel.getText();
		Reader reader = new StringReader(text);
		myHighlightSpace.reset();
		mySyntaxHighlightSpace.reset();
		myJavaLexer.yyreset(reader);
		myJavaLexer.yybegin(myJavaLexer.getDefaultState());
		try {
			int style = myJavaLexer.yylex();
			int startLine = myJavaLexer.getLine();
			int startColumn = myJavaLexer.getColumn();
			while (true) {
				int nextStyle = myJavaLexer.yylex();
				int line = myJavaLexer.getLine();
				int column = myJavaLexer.getColumn();
				myHighlightSpace.highlight(style,
						startLine, startColumn,
						line, column);
				
				style = nextStyle;
				startLine = line;
				startColumn = column;
				if (nextStyle == JavaLexer.YYEOF) break;
				myHighlightSpace.highlight(0,
						startLine, startColumn,
						line, column);
			}
			
			App.post(() -> {
				myJavaModel.highlighting(myHighlightSpace);
			});
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IoUtils.safeClose(reader);
		}
		
		try {
			Compiler compiler;
			if (myCompiler != null) {
				compiler = myCompiler;
			} else {
				CompilerOptions options = new CompilerOptions();
				options.generateClassFiles = false;
				options.reportDeprecationInsideDeprecatedCode = true;
				options.reportDeprecationWhenOverridingDeprecatedMethod = true;
				options.reportUnusedParameterWhenImplementingAbstract = true;
				options.reportUnusedParameterWhenOverridingConcrete = true;
				options.reportUnusedParameterIncludeDocCommentReference = true;
				options.reportUnusedDeclaredThrownExceptionWhenOverriding = true;
				options.reportSpecialParameterHidingField = true;
				options.reportUnavoidableGenericTypeProblems = true;
				options.reportInvalidJavadocTags = true;
				options.reportInvalidJavadocTagsDeprecatedRef = true;
				options.reportInvalidJavadocTagsNotVisibleRef = true;
				options.reportMissingJavadocTagsOverriding = true;
				options.reportMissingJavadocTagsMethodTypeParameters = true;
				options.reportMissingJavadocCommentsOverriding = true;
				options.docCommentSupport = true;
				options.suppressWarnings = true;
				options.performMethodsFullRecovery = true;
				options.performStatementsRecovery = true;
				options.storeAnnotations = false;
				options.processAnnotations = false;
				options.reportMissingOverrideAnnotationForInterfaceMethodImplementation = true;
				options.reportDeadCodeInTrivialIfStatement = false;
				options.ignoreMethodBodies = false;
				options.ignoreSourceFolderWarningOption = false;
				options.includeNullInfoFromAsserts = false;
				options.isAnnotationBasedNullAnalysisEnabled = false;
				options.intendedDefaultNonNullness = 0L;
				options.enableSyntacticNullAnalysisForFields = false;
				options.inheritNullAnnotations = false;
				options.analyseResourceLeaks = true;
				options.reportMissingEnumCaseDespiteDefault = false;
				options.complainOnUninternedIdentityComparison = false;
				options.enablePreviewFeatures = false;
				options.enableJdtDebugCompileMode = false;
				options.ignoreUnnamedModuleForSplitPackage = false;
				
				String sourceLevel = "11";
				String targetLevel = "11";
				options.sourceLevel = CompilerOptions.versionToJdkLevel(sourceLevel);
				options.originalSourceLevel = CompilerOptions.versionToJdkLevel(sourceLevel);
				options.targetJDK = CompilerOptions.versionToJdkLevel(targetLevel);
				options.complianceLevel = CompilerOptions.versionToJdkLevel(targetLevel);
				options.originalComplianceLevel = CompilerOptions.versionToJdkLevel(targetLevel);
				mySyntaxHighlightSpace.reset();
				
				File androidJar = new File(App.getContext().getFilesDir(), "android.jar");
				File coreLambdaJar = new File(App.getContext().getFilesDir(), "core-lambda-stubs.jar");
				if (!androidJar.exists()) {
					InputStream inputStream = App.getContext().getAssets().open("android.jar");
					FileOutputStream outputStream = new FileOutputStream(androidJar);
					try {
						IoUtils.copy(inputStream, outputStream);
					} finally {
						IoUtils.safeClose(inputStream, outputStream);
					}
				}
				if (!coreLambdaJar.exists()) {
					InputStream inputStream = App.getContext().getAssets().open("core-lambda-stubs.jar");
					FileOutputStream outputStream = new FileOutputStream(coreLambdaJar);
					try {
						IoUtils.copy(inputStream, outputStream);
					} finally {
						IoUtils.safeClose(inputStream, outputStream);
					}
				}
				
				var fileSystem = new FileSystem(new String[]{
						androidJar.getAbsolutePath(),
						coreLambdaJar.getAbsolutePath()
				}, new String[0], "UTF-8");
				
				compiler = new Compiler(fileSystem, new IErrorHandlingPolicy() {
					@Override
					public boolean proceedOnErrors() {
						return true;
					}
					
					@Override
					public boolean stopOnFirstError() {
						return false;
					}
					
					@Override
					public boolean ignoreAllErrors() {
						return false;
					}
				}, options, compilationResult -> {
				
				}, new DefaultProblemFactory(Locale.SIMPLIFIED_CHINESE));
				myCompiler = compiler;
			}
			
			myCompiler.reset();
			
			File file = new File(App.getContext().getFilesDir().getAbsolutePath() + "/project/Main.java");
			
			var compilationUnit = new CompilationUnit(text.toCharArray(), file.getAbsolutePath(), "UTF-8");
			
			var resolve = compiler.resolve(compilationUnit, true, true, false);
			var result = resolve.compilationResult;
			
			
		//	var astVisitor = new ASTVisitor() {
				/*@Override
				public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
					int offset = methodDeclaration.sourceStart;
					int endOffset = methodDeclaration.sourceEnd;
					AppLog.i("Method " + myJavaModel.getText(offset, endOffset - offset));
					
					return super.visit(methodDeclaration, scope);
				}
				
				@Override
				public boolean visit(FieldDeclaration fieldDeclaration, MethodScope scope) {
					int offset = fieldDeclaration.nameSourceStart();
					int endOffset = fieldDeclaration.nameSourceEnd();
					int line = myJavaModel.getLineAtOffset(offset);
					int start = myJavaModel.getLineStart(line);
					int column = offset - start;
					int endColumn = endOffset - start;
					mySyntaxHighlightSpace.highlight(FieldStyle,
							line, column, line, endColumn + 1);
					
					return super.visit(fieldDeclaration, scope);
				}
				
				
				@Override
				public boolean visit(FieldReference fieldReference, BlockScope scope) {
					int offset = fieldReference.nameSourceStart();
					int endOffset = fieldReference.nameSourceEnd();
					AppLog.i("FieldRef "+myJavaModel.getText(offset,endOffset-offset));
					return super.visit(fieldReference, scope);
				}
				
				@Override
				public boolean visit(FieldReference fieldReference, ClassScope scope) {
					int offset = fieldReference.nameSourceStart();
					int endOffset = fieldReference.nameSourceEnd();
					AppLog.i("FieldRef "+myJavaModel.getText(offset,endOffset-offset));
					return super.visit(fieldReference, scope);
				}
				
				@Override
				public boolean visit(ImportReference importRef, CompilationUnitScope scope) {
					return super.visit(importRef, scope);
				}
				
				@Override
				public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
					int offset = localDeclaration.nameSourceStart();
					int endOffset = localDeclaration.nameSourceEnd();
					int line = myJavaModel.getLineAtOffset(offset);
					int start = myJavaModel.getLineStart(line);
					int column = offset - start;
					int endColumn = endOffset - start;
					
					// AppLog.i("Local "+myJavaModel.getText(offset,endOffset-offset));
					mySyntaxHighlightSpace.highlight(VariableStyle,
							line, column, line, endColumn + 1);
					return super.visit(localDeclaration, scope);
				}
				
				@Override
				public boolean visit(TypeDeclaration localTypeDeclaration, BlockScope scope) {
					int offset = localTypeDeclaration.sourceStart;
					int endOffset = localTypeDeclaration.sourceEnd;
					AppLog.i("LocalType " + myJavaModel.getText(offset, endOffset - offset));
					
					return super.visit(localTypeDeclaration, scope);
				}
				
				@Override
				public boolean visit(TypeDeclaration memberTypeDeclaration, ClassScope scope) {
					int offset = memberTypeDeclaration.sourceStart;
					int endOffset = memberTypeDeclaration.sourceEnd;
					AppLog.i("MemberType " + myJavaModel.getText(offset, endOffset - offset));
					return super.visit(memberTypeDeclaration, scope);
				}
				
				@Override
				public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
					int offset = typeDeclaration.sourceStart;
					int endOffset = typeDeclaration.sourceEnd;
					AppLog.i("Type " + myJavaModel.getText(offset, endOffset - offset));
					return super.visit(typeDeclaration, scope);
				}*/
			/*
				@Override
				public boolean visit(CompilationUnitDeclaration compilationUnitDeclaration, CompilationUnitScope scope) {
					return super.visit(compilationUnitDeclaration, scope);
				}*/
				
				/*@Override
				public boolean visit(QualifiedAllocationExpression qualifiedAllocationExpression, BlockScope scope) {
					int offset = qualifiedAllocationExpression.nameSourceStart();
					int endOffset = qualifiedAllocationExpression.nameSourceEnd();
					AppLog.i("Allocation " + myJavaModel.getText(offset, endOffset - offset));
					return super.visit(qualifiedAllocationExpression, scope);
				}
				
				@Override
				public boolean visit(SingleNameReference singleNameReference, BlockScope scope) {
					int offset = singleNameReference.nameSourceStart();
					int endOffset = singleNameReference.nameSourceEnd();
					int line = myJavaModel.getLineAtOffset(offset);
					int start = myJavaModel.getLineStart(line);
					int column = offset - start;
					int endColumn = endOffset - start;
					if (singleNameReference.isTypeReference()){
						mySyntaxHighlightSpace.highlight(TypeStyle,
								line, column, line, endColumn + 1);
					}
					
					if (singleNameReference.isExactMethodReference()){
						mySyntaxHighlightSpace.highlight(FunctionCallStyle,
								line, column, line, endColumn + 1);
					}
					AppLog.i("SingleNameRef " + myJavaModel.getText(offset, endOffset - offset));
					return super.visit(singleNameReference, scope);
				}*/
				
			/*	@Override
				public boolean visit(SingleTypeReference singleTypeReference, BlockScope scope) {
					int offset = singleTypeReference.sourceStart;
					int endOffset = singleTypeReference.sourceEnd;
					int line = myJavaModel.getLineAtOffset(offset);
					int start = myJavaModel.getLineStart(line);
					int column = offset - start;
					int endColumn = endOffset - start;
					if (singleTypeReference.isTypeReference()){
						mySyntaxHighlightSpace.highlight(TypeStyle,
								line, column, line, endColumn + 1);
					}
					if (singleTypeReference.isExactMethodReference()){
						mySyntaxHighlightSpace.highlight(FunctionCallStyle,
								line, column, line, endColumn + 1);
					}
					//AppLog.i("SingleTypeRef " + myJavaModel.getText(offset, endOffset - offset));
					return super.visit(singleTypeReference, scope);
				}
				
				@Override
				public boolean visit(SingleTypeReference singleTypeReference, ClassScope scope) {
					int offset = singleTypeReference.sourceStart;
					int endOffset = singleTypeReference.sourceEnd;
					int line = myJavaModel.getLineAtOffset(offset);
					int start = myJavaModel.getLineStart(line);
					int column = offset - start;
					int endColumn = endOffset - start;
					if (singleTypeReference.isTypeReference()){
						mySyntaxHighlightSpace.highlight(TypeStyle,
								line, column, line, endColumn + 1);
					}
					if (singleTypeReference.isExactMethodReference()){
						mySyntaxHighlightSpace.highlight(FunctionCallStyle,
								line, column, line, endColumn + 1);
					}
					//AppLog.i("SingleTypeRef " + myJavaModel.getText(offset, endOffset - offset));
					return super.visit(singleTypeReference, scope);
				}*/
		//	};
			
			
			//resolve.traverse(astVisitor, resolve.scope);
	
			
			for (TypeDeclaration type : resolve.types) {
				AppLog.i(type.toString());
			}
			var errors = result.getAllProblems();
			var syntaxErrors = new ArrayList<Error>();
			
			if (errors != null) {
				for (CategorizedProblem error : errors) {
					if (error instanceof DefaultProblem problem) {
						int startLine = problem.getSourceLineNumber();
						int startColumn = problem.column;
						int endLine = problem.getSourceLineNumber();
						int endColumn = problem.column + (problem.getSourceEnd() - problem.getSourceStart());
						if (problem.isError()) {
							syntaxErrors.add(new Error(startLine, startColumn, endLine, endColumn, Error.ERROR));
						} else if (problem.isWarning() || problem.isInfo()) {
							if (problem.getCategoryID() == 110) {
								syntaxErrors.add(new Error(startLine, startColumn, endLine, endColumn, Error.DEPRECATED));
							} else {
								syntaxErrors.add(new Error(startLine, startColumn, endLine, endColumn, Error.WARNING));
							}
						}
					}
				}
			}
			App.post(new Runnable() {
				@Override
				public void run() {
					myJavaModel.semanticHighlighting(mySyntaxHighlightSpace);
				}
			});
			myErrorListener.syntaxErrors(syntaxErrors);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void highlightSmali() {
		myHighlightSpace.reset();
		mySyntaxHighlightSpace.reset();
		Reader reader = new StringReader(mySmaliModel.getText());
		myParser.reset();
		myParser.setVerboseErrors(false);
		myParser.setAllowOdex(false);
		myParser.setApiLevel(28);
		mySmaliLexer.yyreset(reader);
		mySmaliLexer.yybegin(0);
		mySmaliLexer.setSuppressErrors(true);
		try {
			Token token = mySmaliLexer.yylex();
			int style = token.getType();
			int startLine = mySmaliLexer.getLine() - 1;
			int startColumn = mySmaliLexer.getColumn();
			while (true) {
				Token nextToken = mySmaliLexer.yylex();
				int nextStyle = nextToken.getType();
				int line = mySmaliLexer.getLine() - 1;
				int column = mySmaliLexer.getColumn();
				myHighlightSpace.highlight(getStyle(style),
						startLine, startColumn,
						line, column);
				
				style = nextStyle;
				startLine = line;
				startColumn = column;
				if (nextStyle == -1) break;
				myHighlightSpace.highlight(0,
						startLine, startColumn,
						line, column);
			}
			App.post(() -> {
				mySmaliModel.highlighting(myHighlightSpace);
			});
			
			reader.reset();
			mySmaliLexer.yyreset(reader);
			mySmaliLexer.yybegin(0);
			mySmaliLexer.setSuppressErrors(true);
			CommonTokenStream tokens = new CommonTokenStream(mySmaliLexer);
			myParser.setTokenStream(tokens);
			myParser.setBacktrackingLevel(1);
			smali_file_return smaliFile = myParser.smali_file();
			CommonTree tree = smaliFile.getTree();
			
			TreeVisitor visitor = new TreeVisitor() {
				@Override
				public Object visit(Object o, TreeVisitorAction treeVisitorAction) {
					CommonTree tree = (CommonTree) o;
					visitTree(tree);
					return super.visit(o, treeVisitorAction);
				}
			};
			
			visitor.visit(tree, new TreeVisitorAction() {
				@Override
				public Object pre(Object o) {
					return o;
				}
				
				@Override
				public Object post(Object o) {
					return o;
				}
			});
			
			App.post(() -> {
				mySmaliModel.semanticHighlighting(mySyntaxHighlightSpace);
			});
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			IoUtils.safeClose(reader);
		}
	}
	
	private void visitTree(CommonTree tree) {
		switch (tree.getType()) {
			
			case I_ANNOTATION_ELEMENT -> {
				for (Object child : tree.getChildren()) {
					CommonTree commonTree = (CommonTree) child;
					switch (commonTree.getType()) {
						case SIMPLE_NAME -> {
							Token token = commonTree.getToken();
							int startLine = token.getLine() - 1;
							int startColumn = token.getCharPositionInLine();
							int endLine = token.getLine() - 1;
							int endColumn = startColumn + token.getText().length();
							mySyntaxHighlightSpace.highlight(TypeStyle,
									startLine,
									startColumn,
									endLine,
									endColumn);
						}
					}
				}
			}
			
			case I_LABEL -> {
				for (Object child : tree.getChildren()) {
					CommonTree commonTree = (CommonTree) child;
					switch (commonTree.getType()) {
						case SIMPLE_NAME -> {
							Token token = commonTree.getToken();
							int startLine = token.getLine() - 1;
							int startColumn = token.getCharPositionInLine();
							int endLine = token.getLine() - 1;
							int endColumn = startColumn + token.getText().length();
							mySyntaxHighlightSpace.highlight(MetadataStyle,
									startLine,
									startColumn,
									endLine,
									endColumn);
						}
					}
				}
			}
			
			case CLASS_DESCRIPTOR -> {
				Token token = tree.getToken();
				int startLine = token.getLine() - 1;
				int startColumn = token.getCharPositionInLine();
				int endLine = token.getLine() - 1;
				int endColumn = startColumn + token.getText().length();
				mySyntaxHighlightSpace.highlight(TypeStyle,
						startLine,
						startColumn,
						endLine,
						endColumn);
			}
			
			
			case I_STATEMENT_FORMAT21c_FIELD,
					I_STATEMENT_FORMAT22c_FIELD,
					I_FIELD -> {
				for (Object child : tree.getChildren()) {
					CommonTree commonTree = (CommonTree) child;
					switch (commonTree.getType()) {
						case SIMPLE_NAME -> {
							Token token = commonTree.getToken();
							int startLine = token.getLine() - 1;
							int startColumn = token.getCharPositionInLine();
							int endLine = token.getLine() - 1;
							int endColumn = startColumn + token.getText().length();
							mySyntaxHighlightSpace.highlight(VariableStyle,
									startLine,
									startColumn,
									endLine,
									endColumn);
						}
					}
				}
				
			}
			
			case I_METHOD -> {
				for (Object child : tree.getChildren()) {
					CommonTree commonTree = (CommonTree) child;
					switch (commonTree.getType()) {
						case SIMPLE_NAME -> {
							Token token = commonTree.getToken();
							int startLine = token.getLine() - 1;
							int startColumn = token.getCharPositionInLine();
							int endLine = token.getLine() - 1;
							int endColumn = startColumn + token.getText().length();
							mySyntaxHighlightSpace.highlight(FunctionStyle,
									startLine,
									startColumn,
									endLine,
									endColumn);
						}
					}
				}
			}
			case I_CATCH, I_CATCHALL -> {
				for (Object child : tree.getChildren()) {
					CommonTree commonTree = (CommonTree) child;
					switch (commonTree.getType()) {
						case SIMPLE_NAME -> {
							Token token = commonTree.getToken();
							int startLine = token.getLine() - 1;
							int startColumn = token.getCharPositionInLine();
							int endLine = token.getLine() - 1;
							int endColumn = startColumn + token.getText().length();
							mySyntaxHighlightSpace.highlight(MetadataStyle,
									startLine,
									startColumn,
									endLine,
									endColumn);
						}
					}
				}
			}
/*       case  I_STATEMENT_ARRAY_DATA,
           I_STATEMENT_FORMAT10t ,
           I_STATEMENT_FORMAT10x ,
           I_STATEMENT_FORMAT11n ,
           I_STATEMENT_FORMAT11x ,
           I_STATEMENT_FORMAT12x ,
           I_STATEMENT_FORMAT20bc,
           I_STATEMENT_FORMAT20t ,
           I_STATEMENT_FORMAT21c_METHOD_HANDLE ,
           I_STATEMENT_FORMAT21c_METHOD_TYPE ,
           I_STATEMENT_FORMAT21c_STRING,
           I_STATEMENT_FORMAT21c_TYPE,
           I_STATEMENT_FORMAT21ih,
           I_STATEMENT_FORMAT21lh,
           I_STATEMENT_FORMAT21s ,
           I_STATEMENT_FORMAT21t ,
           I_STATEMENT_FORMAT22b ,
           I_STATEMENT_FORMAT22c_TYPE ,
           I_STATEMENT_FORMAT22s ,
           I_STATEMENT_FORMAT22t ,
           I_STATEMENT_FORMAT22x ,
           I_STATEMENT_FORMAT23x ,
           I_STATEMENT_FORMAT30t ,
           I_STATEMENT_FORMAT31c ,
           I_STATEMENT_FORMAT31i ,
           I_STATEMENT_FORMAT31t ,
           I_STATEMENT_FORMAT32x ,
           I_STATEMENT_FORMAT35c_CALL_SITE ,
           I_STATEMENT_FORMAT35c_TYPE,
           I_STATEMENT_FORMAT3rc_CALL_SITE,
           I_STATEMENT_FORMAT3rc_TYPE,
           I_STATEMENT_FORMAT51l ,
           I_STATEMENT_PACKED_SWITCH ,
           I_STATEMENT_SPARSE_SWITCH->{*/
			
			case I_STATEMENT_FORMAT10t,
					I_STATEMENT_FORMAT21t,
					I_STATEMENT_FORMAT22t -> {
				for (Object child : tree.getChildren()) {
					CommonTree commonTree = (CommonTree) child;
					switch (commonTree.getType()) {
						case SIMPLE_NAME -> {
							Token token = commonTree.getToken();
							int startLine = token.getLine() - 1;
							int startColumn = token.getCharPositionInLine();
							int endLine = token.getLine() - 1;
							int endColumn = startColumn + token.getText().length();
							mySyntaxHighlightSpace.highlight(MetadataStyle,
									startLine,
									startColumn,
									endLine,
									endColumn);
						}
					}
				}
			}
			
			case I_STATEMENT_FORMAT4rcc_METHOD,
					I_STATEMENT_FORMAT3rc_METHOD,
					I_STATEMENT_FORMAT35c_METHOD,
					I_STATEMENT_FORMAT45cc_METHOD -> {
				for (Object child : tree.getChildren()) {
					CommonTree commonTree = (CommonTree) child;
					switch (commonTree.getType()) {
						case SIMPLE_NAME -> {
							Token token = commonTree.getToken();
							int startLine = token.getLine() - 1;
							int startColumn = token.getCharPositionInLine();
							int endLine = token.getLine() - 1;
							int endColumn = startColumn + token.getText().length();
							mySyntaxHighlightSpace.highlight(FunctionCallStyle,
									startLine,
									startColumn,
									endLine,
									endColumn);
						}
					}
				}
			}
			
			default -> {
				if (tree.getChildCount() == 0) return;
				
				for (Object child : tree.getChildren()) {
					CommonTree commonTree = (CommonTree) child;
					switch (commonTree.getType()) {
						case SIMPLE_NAME -> {
                          /*  Token token = commonTree.getToken();
                            CommonTree parent = (CommonTree) commonTree.getParent();*/
							//   AppLog.i("Name " + token.getText() + " Parent " + parent.getText());
						}
						case MEMBER_NAME -> {
                      /*      Token token = commonTree.getToken();
                            CommonTree parent = (CommonTree) commonTree.getParent();*/
							// AppLog.i("Member Name " + token.getText() + " Parent " + parent.getText());
						}
						case CHAR_LITERAL, STRING_LITERAL -> {
							Token token = commonTree.getToken();
							int startLine = token.getLine() - 1;
							int startColumn = token.getCharPositionInLine();
							int endLine = token.getLine() - 1;
							int endColumn = startColumn + token.getText().length();
							mySyntaxHighlightSpace.highlight(StringStyle,
									startLine,
									startColumn,
									endLine,
									endColumn);
						}
					}
				}
			}
		}
	}
	
	private int getStyle(int type) {
		switch (type) {
			case CLASS_DIRECTIVE:
			case SUPER_DIRECTIVE:
			case IMPLEMENTS_DIRECTIVE:
			case SOURCE_DIRECTIVE:
			case FIELD_DIRECTIVE:
			case END_FIELD_DIRECTIVE:
			case SUBANNOTATION_DIRECTIVE:
			case END_SUBANNOTATION_DIRECTIVE:
			case ANNOTATION_DIRECTIVE:
			case END_ANNOTATION_DIRECTIVE:
			case ENUM_DIRECTIVE:
			case METHOD_DIRECTIVE:
			case END_METHOD_DIRECTIVE:
			case REGISTERS_DIRECTIVE:
			case LOCALS_DIRECTIVE:
			case ARRAY_DATA_DIRECTIVE:
			case END_ARRAY_DATA_DIRECTIVE:
			case PACKED_SWITCH_DIRECTIVE:
			case END_PACKED_SWITCH_DIRECTIVE:
			case SPARSE_SWITCH_DIRECTIVE:
			case END_SPARSE_SWITCH_DIRECTIVE:
			case CATCH_DIRECTIVE:
			case CATCHALL_DIRECTIVE:
			case LINE_DIRECTIVE:
			case PARAMETER_DIRECTIVE:
			case END_PARAMETER_DIRECTIVE:
			case LOCAL_DIRECTIVE:
			case END_LOCAL_DIRECTIVE:
			case RESTART_LOCAL_DIRECTIVE:
			case PROLOGUE_DIRECTIVE:
			case EPILOGUE_DIRECTIVE:
			
			case ANNOTATION_VISIBILITY:
			case ACCESS_SPEC:
			case HIDDENAPI_RESTRICTION:
			case VERIFICATION_ERROR_TYPE:
			case INLINE_INDEX:
			case VTABLE_INDEX:
			case FIELD_OFFSET:
			case METHOD_HANDLE_TYPE_FIELD:
			case METHOD_HANDLE_TYPE_METHOD:
			
			case INSTRUCTION_FORMAT10t:
			case INSTRUCTION_FORMAT10x:
			case INSTRUCTION_FORMAT10x_ODEX:
			case INSTRUCTION_FORMAT11n:
			case INSTRUCTION_FORMAT11x:
			case INSTRUCTION_FORMAT12x_OR_ID:
			case INSTRUCTION_FORMAT12x:
			case INSTRUCTION_FORMAT20bc:
			case INSTRUCTION_FORMAT20t:
			case INSTRUCTION_FORMAT21c_FIELD:
			case INSTRUCTION_FORMAT21c_FIELD_ODEX:
			case INSTRUCTION_FORMAT21c_STRING:
			case INSTRUCTION_FORMAT21c_TYPE:
			case INSTRUCTION_FORMAT21c_METHOD_HANDLE:
			case INSTRUCTION_FORMAT21c_METHOD_TYPE:
			case INSTRUCTION_FORMAT21ih:
			case INSTRUCTION_FORMAT21lh:
			case INSTRUCTION_FORMAT21s:
			case INSTRUCTION_FORMAT21t:
			case INSTRUCTION_FORMAT22b:
			case INSTRUCTION_FORMAT22c_FIELD:
			case INSTRUCTION_FORMAT22c_FIELD_ODEX:
			case INSTRUCTION_FORMAT22c_TYPE:
			case INSTRUCTION_FORMAT22cs_FIELD:
			case INSTRUCTION_FORMAT22s_OR_ID:
			case INSTRUCTION_FORMAT22s:
			case INSTRUCTION_FORMAT22t:
			case INSTRUCTION_FORMAT22x:
			case INSTRUCTION_FORMAT23x:
			case INSTRUCTION_FORMAT30t:
			case INSTRUCTION_FORMAT31c:
			case INSTRUCTION_FORMAT31i_OR_ID:
			case INSTRUCTION_FORMAT31i:
			case INSTRUCTION_FORMAT31t:
			case INSTRUCTION_FORMAT32x:
			case INSTRUCTION_FORMAT35c_CALL_SITE:
			case INSTRUCTION_FORMAT35c_METHOD:
			case INSTRUCTION_FORMAT35c_METHOD_OR_METHOD_HANDLE_TYPE:
			case INSTRUCTION_FORMAT35c_METHOD_ODEX:
			case INSTRUCTION_FORMAT35c_TYPE:
			case INSTRUCTION_FORMAT35mi_METHOD:
			case INSTRUCTION_FORMAT35ms_METHOD:
			case INSTRUCTION_FORMAT3rc_CALL_SITE:
			case INSTRUCTION_FORMAT3rc_METHOD:
			case INSTRUCTION_FORMAT3rc_METHOD_ODEX:
			case INSTRUCTION_FORMAT3rc_TYPE:
			case INSTRUCTION_FORMAT3rmi_METHOD:
			case INSTRUCTION_FORMAT3rms_METHOD:
			case INSTRUCTION_FORMAT45cc_METHOD:
			case INSTRUCTION_FORMAT4rcc_METHOD:
			case INSTRUCTION_FORMAT51l:
				
				return KeywordStyle;
			case PARAM_LIST_OR_ID_PRIMITIVE_TYPE:
			case PRIMITIVE_TYPE:
			case VOID_TYPE:
			case CLASS_DESCRIPTOR:
			case ARRAY_TYPE_PREFIX:
				return TypeStyle;
			
			case DOTDOT:
			case ARROW:
			case EQUAL:
			case COLON:
			case COMMA:
			case OPEN_BRACE:
			case CLOSE_BRACE:
			case OPEN_PAREN:
			case CLOSE_PAREN:
				return SeparatorStyle;
			
			case POSITIVE_INTEGER_LITERAL:
			case NEGATIVE_INTEGER_LITERAL:
			case LONG_LITERAL:
			case SHORT_LITERAL:
			case BYTE_LITERAL:
			case FLOAT_LITERAL_OR_ID:
			case DOUBLE_LITERAL_OR_ID:
			case FLOAT_LITERAL:
			case DOUBLE_LITERAL:
				return NumberStyle;
			case BOOL_LITERAL:
			case NULL_LITERAL:
				return StringStyle;
			case LINE_COMMENT:
				return CommentStyle;
			case WHITE_SPACE:
			default:
				return PlainStyle;
		}
	}
	
	public void compile(HighlightModel model, CompilerCallback callback) {
		synchronized (myLock) {
			myCompile = true;
			myCompilerCallback = callback;
			myJavaModel = model;
			myLock.notify();
		}
	}
	
	public void highlightJava(HighlightModel model) {
		synchronized (myLock) {
			myHighlight = true;
			myHighlightJava = true;
			myJavaModel = model;
			myLock.notify();
		}
	}
	
	public void highlightSmali(HighlightModel model) {
		synchronized (myLock) {
			myHighlight = true;
			myHighlightSmali = true;
			mySmaliModel = model;
			myLock.notify();
		}
	}
	
	public void shutdown() {
		synchronized (myLock) {
			myShutdown = true;
			myLock.notify();
		}
	}
	
	private void internalCompile(File androidJar, File coreLambdaJar, File mainJava, String sourceLevel, String targetLevel, String destinationPath) throws Exception {
		var output = new ByteArrayOutputStream();
		try {
			var outputPrinter = new PrintWriter(output);
			var compiler = new EclipseCompiler();
			DiagnosticListener<JavaFileObject> diagnosticListener = (diagnostic) -> {
				int line = (int) diagnostic.getLineNumber();
				int column = (int) diagnostic.getColumnNumber();
				int endColumn = (int) (column + (diagnostic.getEndPosition() - diagnostic.getStartPosition()));
				String msg = diagnostic.getMessage(Locale.getDefault());
				//  Log.d(LOG_TAG, "compile diagnostic: (" + line + "," + column + "," + line + "," + endColumn + ") " + msg);
			};
			
			var options = new ArrayList<String>();
			options.add("-source");
			options.add(sourceLevel);
			options.add("-target");
			options.add(targetLevel);
			
			var fileSystem = compiler.getStandardFileManager(diagnosticListener, Locale.getDefault(), Charset.defaultCharset());
			var platformClassPaths = new ArrayList<File>();
			platformClassPaths.add(androidJar);
			platformClassPaths.add(coreLambdaJar);
			var classPaths = new ArrayList<File>();
			var sourcePaths = new ArrayList<File>();
			sourcePaths.add(mainJava);
			
			fileSystem.setLocation(PLATFORM_CLASS_PATH, platformClassPaths);
			fileSystem.setLocation(CLASS_PATH, classPaths);
			fileSystem.setLocation(CLASS_OUTPUT, List.of(new File(destinationPath)));
			
			var task = compiler.getTask(
					outputPrinter,
					fileSystem,
					diagnosticListener,
					options,
					null,
					fileSystem.getJavaFileObjectsFromFiles(sourcePaths));
			
			if (!task.call()) {
				// Log.d(LOG_TAG, "compile: error " + output);
				throw new Exception(output.toString());
			}
		} finally {
			IoUtils.safeClose(output);
		}
	}
	
	private void findFile(File dir, String ext, List<File> files) {
		if (dir.isDirectory()) {
			var list = dir.listFiles();
			if (list != null) {
				for (File file : list) {
					findFile(file, ext, files);
				}
			}
		} else if (dir.isFile() && dir.getName().endsWith(ext)) {
			files.add(dir);
		}
	}
	
	public interface CompilerCallback {
		void compileSuccessful(List<String> filePaths);
		
		void compileFailure(String msg);
	}
	
	public interface ErrorListener {
		void syntaxErrors(List<Error> errors);
	}
	
	public static class Error {
		public static final int ERROR = 0;
		public static final int WARNING = 1;
		public static final int DEPRECATED = 2;
		public int startLine;
		public int startColumn;
		public int endLine;
		public int endColumn;
		public int level;
		
		public Error(int startLine, int startColumn, int endLine, int endColumn, int level) {
			this.startLine = startLine;
			this.startColumn = startColumn;
			this.endLine = endLine;
			this.endColumn = endColumn;
			this.level = level;
		}
	}
	

}
