/*
 * THIS SOURCE FILE IS PART OF JBASIC, AN OPEN SOURCE PUBLICLY AVAILABLE
 * JAVA SOFTWARE PACKAGE HOSTED BY SOURCEFORGE.NET
 * 
 * THIS SOFTWARE IS PROVIDED VIA THE GNU PUBLIC LICENSE AND IS FREELY
 * AVAILABLE FOR ANY PURPOSE COMMERCIAL OR OTHERWISE AS LONG AS THE AUTHORSHIP
 * AND COPYRIGHT INFORMATION IS RETAINED INTACT AND APPROPRIATELY VISIBLE
 * TO THE END USER.
 * 
 * SEE THE PROJECT FILE AT HTTP://WWW.SOURCEFORGE.NET/PROJECTS/JBASIC FOR
 * MORE INFORMATION.
 * 
 * COPYRIGHT 2003-2011 BY TOM COLE, TOMCOLE@USERS.SF.NET
 *
 */
package org.fernwood.jbasic.opcodes;

import org.fernwood.jbasic.Status;
import org.fernwood.jbasic.runtime.ArgumentList;
import org.fernwood.jbasic.runtime.ByteCode;
import org.fernwood.jbasic.runtime.JBasicException;

/**
 * Abstract definition of an OpCode object. Each actual subclass defines a
 * specific bytecode operation. This abstract class is the shell for all opcode
 * values.
 * 
 * @author tom
 * @version version 1.0 Aug 15, 2006
 * 
 */
public abstract class AbstractOpcode {

	public String toString() {
		return "execute() object for " + AbstractOpcode.getName(opCode);
	}

	int opCode;

	/**
	 * Initialize the array of opcode values. This is done once as part of
	 * initialization, and uses reflection to dynamically learn the names of the
	 * actual OpCode classes in the current execution environment. This array is
	 * created to support dispatching the actual execution module of each opcode
	 * when a bytecode operator is found.
	 * 
	 * @return An array of OpCode objects, indexed by the opcode;s integer
	 *         bytecode value.
	 */
	public static AbstractOpcode[] initialize() {

		final int ix = nameMap.length;
		final AbstractOpcode[] dispatch = new AbstractOpcode[ix + 10];

		for (int i = 0; i < ix; i++) {
			final OpCodeDef def = nameMap[i];

			try {
				final String aClass = "org.fernwood.jbasic.opcodes.Op"
						+ def.name.substring(1);
				final Class c = Class.forName(aClass);
				final AbstractOpcode op = (AbstractOpcode) c.newInstance();
				op.opCode = def.opCode;
				int xop = def.opCode;
				if( xop > ByteCode._BRANCH_FLAG)
					xop = xop - ByteCode._BRANCH_FLAG;
				dispatch[xop] = op;

			} catch (final Exception e) {
				//System.out.println("DEBUG: Unexpected bytecode init error for " 
				//		+ def.name + ", " + e);
			}

		}
		return dispatch;
	}

	static class OpCodeDef {

		static final int NONE = 0;

		static final int DOUBLE = 1;

		static final int INTEGER = 2;

		static final int STRING = 4;

		int opCode;

		int argumentMask;

		String name;

		String description;

		OpCodeDef(final int n, final String s) {
			opCode = n;
			name = s;
			argumentMask = DOUBLE | INTEGER | STRING;
			description = null;
		}

		OpCodeDef(final int n, final String s, final int mask, final String desc) {
			opCode = n;
			name = s;
			argumentMask = mask;
			description = desc;
		}

		OpCodeDef(final int n, final String s, final int mask) {
			opCode = n;
			name = s;
			argumentMask = mask;
			description = null;
		}
	}

	static final OpCodeDef[] nameMap = {
		new OpCodeDef(ByteCode._DECOMP, "_DECOMP"),
		new OpCodeDef(ByteCode._SCALE, "_SCALE"),
		new OpCodeDef(ByteCode._CATALOG, "_CATALOG"),
		new OpCodeDef(ByteCode._JOIN, "_JOIN"),
		new OpCodeDef(ByteCode._ASSERTTYPE, "_ASSERTTYPE"),
		new OpCodeDef(ByteCode._INSERT, "_INSERT"),
		new OpCodeDef(ByteCode._DUPREF, "_DUPREF"),
		new OpCodeDef(ByteCode._DCLVAR, "_DCLVAR"),
		new OpCodeDef(ByteCode._SETDYNVAR, "_SETDYNVAR"),
		new OpCodeDef(ByteCode._SETSCOPE, "_SETSCOPE"),
		new OpCodeDef(ByteCode._PROTOTYPE, "_PROTOTYPE"),
		new OpCodeDef(ByteCode._TYPECHK, "_TYPECHK"),
		new OpCodeDef(ByteCode._STRPOOL, "_STRPOOL"),
		new OpCodeDef(ByteCode._CONSOLE, "_CONSOLE"),
		new OpCodeDef(ByteCode._SLEEP, "_SLEEP"),
		new OpCodeDef(ByteCode._ROWPROMPT, "_ROWPROMPT"),
		new OpCodeDef(ByteCode._INPUTROW, "_INPUTROW"),
		new OpCodeDef(ByteCode._TABLE, "_TABLE"),
		new OpCodeDef(ByteCode._WHERE, "_WHERE"),
		new OpCodeDef(ByteCode._MIN, "_MIN"),
		new OpCodeDef(ByteCode._MAX, "_MAX"),
		new OpCodeDef(ByteCode._SETPGM, "_SETPGM"),
		new OpCodeDef(ByteCode._DROP, "_DROP"),
		new OpCodeDef(ByteCode._PACKAGE, "_PACKAGE"),
		new OpCodeDef(ByteCode._SERVER, "_SERVER"),
		new OpCodeDef(ByteCode._LOADFILE, "_LOADFILE"),
		new OpCodeDef(ByteCode._BRLOOP, "_BRLOOP"),
		new OpCodeDef(ByteCode._IF, "_IF"),
		new OpCodeDef(ByteCode._FIELD, "_FIELD"),
		new OpCodeDef(ByteCode._LOGGING, "_LOGGING"),
		new OpCodeDef(ByteCode._CONSTANT, "_CONSTANT"),
		new OpCodeDef(ByteCode._REFSTR, "_REFSTR"),
		new OpCodeDef(ByteCode._ASM, "_ASM"),
		new OpCodeDef(ByteCode._LOCIDX, "_LOCIDX"),
		new OpCodeDef(ByteCode._LOCMEM, "_LOCMEM"),
		new OpCodeDef(ByteCode._LOCREF, "_LOCREF"),
		new OpCodeDef(ByteCode._SET, "_SET"),
		new OpCodeDef(ByteCode._LOADFREF, "_LOADFREF"),
		new OpCodeDef(ByteCode._INPUTXML, "_INPUTXML"),
		new OpCodeDef(ByteCode._TIME, "_TIME"),
		new OpCodeDef(ByteCode._ARGC, "_ARGC"),
		new OpCodeDef(ByteCode._BRNZ, "_BRNZ"),
		new OpCodeDef(ByteCode._SBOX, "_SBOX"),
		new OpCodeDef(ByteCode._NEEDP, "_NEEDP"),
		new OpCodeDef(ByteCode._STORALL, "_STORALL"),
		new OpCodeDef(ByteCode._RAND, "_RAND"),
		new OpCodeDef(ByteCode._FOREACH, "_FOREACH"),
		new OpCodeDef(ByteCode._DEFFN, "_DEFFN"),
		new OpCodeDef(ByteCode._CALLFL, "_CALLFL"),
		new OpCodeDef(ByteCode._SAVE, "_SAVE"),
		new OpCodeDef(ByteCode._RIGHT, "_RIGHT"),
		new OpCodeDef(ByteCode._LEFT, "_LEFT"),
		new OpCodeDef(ByteCode._OF, "_OF"),
		new OpCodeDef(ByteCode._CHAIN, "_CHAIN"),
		new OpCodeDef(ByteCode._COMMON, "_COMMON"),
		new OpCodeDef(ByteCode._GOSUB, "_GOSUB"),
		new OpCodeDef(ByteCode._GOTO, "_GOTO"),
		new OpCodeDef(ByteCode._EOF, "_EOF"),
		new OpCodeDef(ByteCode._NOTEOF, "_NOTEOF"),
		new OpCodeDef(ByteCode._OVER, "_OVER"),
		new OpCodeDef(ByteCode._SUBSTR, "_SUBSTR"),
		new OpCodeDef(ByteCode._THREAD, "_THREAD"),
		new OpCodeDef(ByteCode._LOCK, "_LOCK"),
		new OpCodeDef(ByteCode._UNLOCK, "_UNLOCK"),
		new OpCodeDef(ByteCode._DEFMSG, "_DEFMSG"),
		new OpCodeDef(ByteCode._TRACE, "_TRACE"),
		new OpCodeDef(ByteCode._DEBUG, "_DEBUG"),
		new OpCodeDef(ByteCode._SYS, "_SYS"),
		new OpCodeDef(ByteCode._CALLT, "_CALLT"),
		new OpCodeDef(ByteCode._TYPES, "_TYPES", OpCodeDef.INTEGER),
		new OpCodeDef(ByteCode._INCR, "_INCR", OpCodeDef.INTEGER
				| OpCodeDef.STRING),
				new OpCodeDef(ByteCode._FORX, "_FORX"),
				new OpCodeDef(ByteCode._NL, "_NL", OpCodeDef.INTEGER),
				new OpCodeDef(ByteCode._SIZEOF, "_SIZEOF", OpCodeDef.INTEGER),
				new OpCodeDef(ByteCode._EXP, "_EXP"),
				new OpCodeDef(ByteCode._SEEK, "_SEEK"),
				new OpCodeDef(ByteCode._GET, "_GET"),
				new OpCodeDef(ByteCode._PUT, "_PUT"),
				new OpCodeDef(ByteCode._VALUE, "_VALUE", OpCodeDef.INTEGER),
				new OpCodeDef(ByteCode._CLEAR, "_CLEAR", OpCodeDef.STRING),
				new OpCodeDef(ByteCode._READ, "_READ", OpCodeDef.STRING),
				new OpCodeDef(ByteCode._REW, "_REW", OpCodeDef.NONE),
				new OpCodeDef(ByteCode._EOD, "_EOD", OpCodeDef.NONE),
				new OpCodeDef(ByteCode._DATA, "_DATA", OpCodeDef.INTEGER),
				new OpCodeDef(ByteCode._UID, "_UID", OpCodeDef.NONE),
				new OpCodeDef(ByteCode._ERROR, "_ERROR", OpCodeDef.STRING),
				new OpCodeDef(ByteCode._LABEL, "_LABEL", OpCodeDef.STRING),
				new OpCodeDef(ByteCode._LOADREG, "_LOADREG", OpCodeDef.INTEGER),
				new OpCodeDef(ByteCode._STORREG, "_STORREG", OpCodeDef.INTEGER),
				new OpCodeDef(ByteCode._SIGN, "_SIGN"),
				new OpCodeDef(ByteCode._FOR, "_FOR", OpCodeDef.INTEGER
						| OpCodeDef.STRING),
						new OpCodeDef(ByteCode._NEXT, "_NEXT", OpCodeDef.INTEGER
								| OpCodeDef.STRING),
								new OpCodeDef(ByteCode._LENGTH, "_LENGTH"),
								new OpCodeDef(ByteCode._RECORD, "_RECORD"),
								new OpCodeDef(ByteCode._STORR, "_STORR"),
								new OpCodeDef(ByteCode._LOADR, "_LOADR"),
								new OpCodeDef(ByteCode._STMT, "_STMT", OpCodeDef.INTEGER
										| OpCodeDef.STRING),
										new OpCodeDef(ByteCode._PROT, "_PROT", OpCodeDef.STRING),
										new OpCodeDef(ByteCode._DUP, "_DUP"),
										new OpCodeDef(ByteCode._QUIT, "_QUIT"),
										new OpCodeDef(ByteCode._BR, "_BR"),
										new OpCodeDef(ByteCode._CHAR, "_CHAR", OpCodeDef.INTEGER),
										new OpCodeDef(ByteCode._SIGNAL, "_SIGNAL"),
										new OpCodeDef(ByteCode._LOAD, "_LOAD", OpCodeDef.INTEGER
												| OpCodeDef.STRING),
												new OpCodeDef(ByteCode._ARG, "_ARG", OpCodeDef.INTEGER
														| OpCodeDef.STRING),
														new OpCodeDef(ByteCode._ARGDEF, "_ARGDEF", OpCodeDef.INTEGER
																| OpCodeDef.STRING),
																new OpCodeDef(ByteCode._JSBIND, "_JSBIND"),
																new OpCodeDef(ByteCode._CVT, "_CVT", OpCodeDef.INTEGER),
																new OpCodeDef(ByteCode._SWAP, "_SWAP"),
																new OpCodeDef(ByteCode._EXEC, "_EXEC", OpCodeDef.STRING),
																new OpCodeDef(ByteCode._RET, "_RET"),
																new OpCodeDef(ByteCode._STRCMP, "_STRCMP"),
																new OpCodeDef(ByteCode._JMPIND, "_JMPIND"),
																new OpCodeDef(ByteCode._JMP, "_JMP"),
																new OpCodeDef(ByteCode._ARRAY, "_ARRAY", OpCodeDef.INTEGER),
																new OpCodeDef(ByteCode._NEGATE, "_NEGATE"),
																new OpCodeDef(ByteCode._INTEGER, "_INTEGER", OpCodeDef.INTEGER),
																new OpCodeDef(ByteCode._DOUBLE, "_DOUBLE", OpCodeDef.DOUBLE),
																new OpCodeDef(ByteCode._STRING, "_STRING", OpCodeDef.STRING),
																new OpCodeDef(ByteCode._BOOL, "_BOOL", OpCodeDef.INTEGER),
																new OpCodeDef(ByteCode._STOR, "_STOR", OpCodeDef.INTEGER
																		| OpCodeDef.STRING),
																		new OpCodeDef(ByteCode._STORA, "_STORA"),
																		new OpCodeDef(ByteCode._ADD, "_ADD"),
																		new OpCodeDef(ByteCode._SUB, "_SUB"),
																		new OpCodeDef(ByteCode._MULT, "_MULT"),
																		new OpCodeDef(ByteCode._DIV, "_DIV"),
																		new OpCodeDef(ByteCode._CONCAT, "_CONCAT"),
																		new OpCodeDef(ByteCode._BRZ, "_BRZ"),
																		new OpCodeDef(ByteCode._GE, "_GE"),
																		new OpCodeDef(ByteCode._GT, "_GT"),
																		new OpCodeDef(ByteCode._EQ, "_EQ"),
																		new OpCodeDef(ByteCode._NE, "_NE"),
																		new OpCodeDef(ByteCode._LE, "_LE"),
																		new OpCodeDef(ByteCode._LT, "_LT"),
																		new OpCodeDef(ByteCode._CALLF, "_CALLF", OpCodeDef.INTEGER
																				| OpCodeDef.STRING),
																				new OpCodeDef(ByteCode._JSB, "_JSB"),
																				new OpCodeDef(ByteCode._OUT, "_OUT"),
																				new OpCodeDef(ByteCode._OUTTAB, "_OUTTAB"),
																				new OpCodeDef(ByteCode._OUTNL, "_OUTNL"),
																				new OpCodeDef(ByteCode._OPEN, "_OPEN", OpCodeDef.INTEGER
																						| OpCodeDef.STRING),
																						new OpCodeDef(ByteCode._COLUMN, "_COLUMN"),
																						new OpCodeDef(ByteCode._CLOSE, "_CLOSE"),
																						new OpCodeDef(ByteCode._INDEX, "_INDEX"),
																						new OpCodeDef(ByteCode._OR, "_OR"),
																						new OpCodeDef(ByteCode._MOD, "_MOD"),
																						new OpCodeDef(ByteCode._AND, "_AND"),
																						new OpCodeDef(ByteCode._NOT, "_NOT"),
																						new OpCodeDef(ByteCode._END, "_END"),
																						new OpCodeDef(ByteCode._CALLP, "_CALLP"),
																						new OpCodeDef(ByteCode._ENTRY, "_ENTRY"),
																						new OpCodeDef(ByteCode._LINE, "_LINE"),
																						new OpCodeDef(ByteCode._SORT, "_SORT"),
																						new OpCodeDef(ByteCode._NOOP, "_NOOP"),
																						new OpCodeDef(ByteCode._DO, "_DO"),
																						new OpCodeDef(ByteCode._LOOP, "_LOOP"),
																						new OpCodeDef(ByteCode._ALLOC, "_ALLOC"),
																						new OpCodeDef(ByteCode._KILL, "_KILL"),
																						new OpCodeDef(ByteCode._INPUT, "_INPUT"),
																						new OpCodeDef(ByteCode._METHOD, "_METHOD"),
																						new OpCodeDef(ByteCode._OBJECT, "_OBJECT"),
																						new OpCodeDef(ByteCode._CALLM, "_CALLM"),
																						new OpCodeDef(ByteCode._LOADREF, "_LOADREF"),
																						new OpCodeDef(ByteCode._STORINT, "_STORINT"),
																						new OpCodeDef(ByteCode._STORDBL, "_STORDBL"),
																						new OpCodeDef(ByteCode._STORBOOL, "_STORBOOL"),
																						new OpCodeDef(ByteCode._ADDI, "_ADDI"),
																						new OpCodeDef(ByteCode._SUBI, "_SUBI"),
																						new OpCodeDef(ByteCode._MULTI, "_MULTI"),
																						new OpCodeDef(ByteCode._DIVI, "_DIVI"),
																						new OpCodeDef(ByteCode._RESULT, "_RESULT"),
																						new OpCodeDef(ByteCode._USING, "_USING"),
																						new OpCodeDef(ByteCode._DIM, "_DIM", OpCodeDef.INTEGER)

	};

	/**
	 * For a given opCode, return the text name used to represent it in
	 * disassembly output, etc.
	 * 
	 * @param code
	 *            The integer opCode value.
	 * @return A string representation of the name, or _UNKNOWN[code] if the
	 *         code was not valid.
	 */
	public static String getName(final int code) {

		/*
		 * Search the list of byte code name mappings for a match
		 */

		final int len = nameMap.length;
		for (int ix = 0; ix < len; ix++)
			if (nameMap[ix].opCode == code)
				return nameMap[ix].name;
		return "UNKNOWN[" + Integer.toString(code) + "]";
	}

	/**
	 * Given an OpCode name, find out it's integer value.
	 * 
	 * @param name
	 *            The OpCode name, which must be in UPPERCASE text.
	 * @return The integer opcode value, or zero if it was not a valid opcode
	 *         name.
	 */
	public static int getCode(final String name) {
		final String localName = name.toUpperCase();
		final int len = nameMap.length;
		for (int ix = 0; ix < len; ix++)
			if (nameMap[ix].name.equals(localName))
				return nameMap[ix].opCode;
		return 0;

	}


	/**
	 * Generate an argument list object from the stack given the
	 * current argument count.
	 * @param env the instruction environment being executed
	 * @return an ArgumentList object containing the correct number
	 * of arguments removed from the stack.
	 * @throws JBasicException in the event of a stack underflow
	 */
	public ArgumentList fetchArgs(final InstructionContext env)
			throws JBasicException {
		ArgumentList funcArgs = new ArgumentList(env.session);

		int argc = env.instruction.integerOperand;
		if( argc < 0 )
			return funcArgs;

		int stackSize;
		int ix;
		stackSize = env.codeStream.stackSize();
		if (stackSize < argc)
			throw new JBasicException(Status.UNDERFLOW);

		for (ix = stackSize - argc; ix < stackSize; ix++) {
			funcArgs.insert(env.codeStream.getStackElement(ix));
		}
		env.codeStream.discard(argc);
		return funcArgs;
	}

	/**
	 * Execute an instruction opcode. This executes a single ByteCode.
	 * 
	 * @param env
	 *            The Opcode Environment object. This tells what the JBasic
	 *            runtime object is, the currently executing ByteCode stream,
	 *            the current instruction arguments, and the active local symbol
	 *            table.
	 * @throws JBasicException that encapsulates any error.
	 */
	public abstract void execute(InstructionContext env) throws JBasicException;
}
