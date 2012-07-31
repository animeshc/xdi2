package xdi2.messaging.target;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdi2.core.Statement;
import xdi2.core.Statement.ContextNodeStatement;
import xdi2.core.exceptions.Xdi2ParseException;
import xdi2.core.impl.AbstractStatement;
import xdi2.core.util.iterators.SelectingClassIterator;
import xdi2.core.xri3.impl.XRI3Segment;
import xdi2.messaging.Message;
import xdi2.messaging.MessageEnvelope;
import xdi2.messaging.MessageResult;
import xdi2.messaging.Operation;
import xdi2.messaging.exceptions.Xdi2MessagingException;
import xdi2.messaging.target.contributor.Contributor;
import xdi2.messaging.target.interceptor.Interceptor;
import xdi2.messaging.target.interceptor.MessageEnvelopeInterceptor;
import xdi2.messaging.target.interceptor.MessageInterceptor;
import xdi2.messaging.target.interceptor.MessagingTargetInterceptor;
import xdi2.messaging.target.interceptor.OperationInterceptor;
import xdi2.messaging.target.interceptor.ResultInterceptor;
import xdi2.messaging.target.interceptor.TargetInterceptor;

/**
 * The AbstractMessagingTarget relieves subclasses from the following:
 * - Implementation of execute() with a message envelope (all messages
 *   in the envelope and all operations in the messages will be executed).
 * - Implementation of execute() with a message (all operations
 *   in the messages will be executed).
 * - Using a list of MessageEnvelopeInterceptors, MessageInterceptors and 
 *   OperationInterceptors
 * - Maintaining an "execution context" object where state can be kept between
 *   individual operations.
 * 
 * Subclasses must do the following:
 * - Implement execute() with an operation.
 * 
 * @author markus
 */
public abstract class AbstractMessagingTarget implements MessagingTarget {

	private static final Logger log = LoggerFactory.getLogger(AbstractMessagingTarget.class);

	private XRI3Segment owner;
	private Map<XRI3Segment, List<Contributor>> contributors;
	private List<Interceptor> interceptors;

	public AbstractMessagingTarget() {

		this.owner = null;
		this.contributors = new HashMap<XRI3Segment, List<Contributor>> ();
		this.interceptors = new ArrayList<Interceptor> ();
	}

	@Override
	public void init() throws Exception {

		// execute interceptors

		for (Iterator<MessagingTargetInterceptor> messagingTargetInterceptors = this.getMessagingTargetInterceptors(); messagingTargetInterceptors.hasNext(); ) {

			MessagingTargetInterceptor messagingTargetInterceptor = messagingTargetInterceptors.next();

			messagingTargetInterceptor.init(this);
		}
	}

	@Override
	public void shutdown() throws Exception {

		// execute interceptors

		for (Iterator<MessagingTargetInterceptor> messagingTargetInterceptors = this.getMessagingTargetInterceptors(); messagingTargetInterceptors.hasNext(); ) {

			MessagingTargetInterceptor messagingTargetInterceptor = messagingTargetInterceptors.next();

			messagingTargetInterceptor.shutdown(this);
		}
	}

	/**
	 * Executes a message envelope by executing all its messages.
	 * @param messageEnvelope The XDI message envelope containing XDI messages to be executed.
	 * @param messageResult The result produced by executing the message envelope.
	 * @param executionContext An "execution context" object that is created when
	 * execution of the message envelope begins and that will be passed into every execute() method.
	 */
	@Override
	public void execute(MessageEnvelope messageEnvelope, MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {

		if (messageEnvelope == null) throw new NullPointerException();
		if (messageResult == null) throw new NullPointerException();
		if (executionContext == null) executionContext = new ExecutionContext(this);

		int i = 0;
		int messageCount = messageEnvelope.getMessageCount();
		int operationCount = messageEnvelope.getOperationCount();

		try {

			// clear execution context

			executionContext.clearMessageEnvelopeAttributes();

			// before message envelope

			this.before(messageEnvelope, executionContext);

			// execute message envelope interceptors (before)

			if (this.executeMessageEnvelopeInterceptorsBefore(messageEnvelope, messageResult, executionContext)) {

				return;
			}

			// execute the message envelope

			for (Iterator<Message> messages = messageEnvelope.getMessages(); messages.hasNext(); ) {

				i++;
				Message message = messages.next();

				// clear execution context

				executionContext.clearMessageAttributes();

				// before message

				this.before(message, executionContext);

				// execute message interceptors (before)

				if (this.executeMessageInterceptorsBefore(message, messageResult, executionContext)) {

					continue;
				}

				// execute the message

				if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Executing message " + i + "/" + messageCount + " (" + operationCount + " operations).");

				this.execute(message, messageResult, executionContext);

				// execute message interceptors (after)

				if (this.executeMessageInterceptorsAfter(message, messageResult, executionContext)) {

					continue;
				}

				// after message

				this.after(message, executionContext);
			}

			// execute message envelope interceptors (after)

			if (this.executeMessageEnvelopeInterceptorsAfter(messageEnvelope, messageResult, executionContext)) {

				return;
			}

			// after message envelope

			this.after(messageEnvelope, executionContext);

			// execute result interceptors

			this.executeResultInterceptorsFinish(messageResult, executionContext);
		} catch (Exception ex) {

			// check exception

			if (! (ex instanceof Xdi2MessagingException)) {

				ex = new Xdi2MessagingException(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(), ex, null);
			}

			// execute message envelope interceptors (exception)

			this.executeMessageEnvelopeInterceptorsException(messageEnvelope, messageResult, executionContext, ex);

			// exception in message envelope

			this.exception(messageEnvelope, executionContext, ex);

			// throw it

			throw (Xdi2MessagingException) ex;
		}
	}

	/**
	 * Executes a message by executing all its operations.
	 * @param message The XDI message containing XDI operations to be executed.
	 * @param messageResult The result produced by executing the message envelope.
	 * @param executionContext An "execution context" object that is created when
	 * execution of the message envelope begins and that will be passed into every execute() method.
	 */
	public void execute(Message message, MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {

		if (message == null) throw new NullPointerException();

		int i = 0;
		int operationCount = message.getOperationCount();

		for (Iterator<Operation> operations = message.getOperations(); operations.hasNext(); ) {

			i++;
			Operation operation = operations.next();
			operation = Operation.castOperation(operation);

			try {

				// clear execution context

				executionContext.clearOperationAttributes();

				// before operation

				this.before(operation, executionContext);

				// execute operation interceptors (before)

				if (this.executeOperationInterceptorsBefore(operation, messageResult, executionContext)) {

					continue;
				}

				// execute the operation

				if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Executing operation " + i + "/" + operationCount + " (" + operation.getOperationXri() + ").");

				this.execute(operation, messageResult, executionContext);

				// execute operation interceptors (after)

				if (this.executeOperationInterceptorsAfter(operation, messageResult, executionContext)) {

					continue;
				}

				// after operation

				this.after(operation, executionContext);
			} catch (Exception ex) {

				// check exception

				if (! (ex instanceof Xdi2MessagingException)) {

					ex = new Xdi2MessagingException(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(), ex, operation);
				}

				// throw it

				throw (Xdi2MessagingException) ex;
			}
		}
	}

	/**
	 * Executes an operation.
	 * @param operation The XDI operation.
	 * @param messageResult The result produced by executing the message envelope.
	 * @param executionContext An "execution context" object that is created when
	 * execution of the message envelope begins and that will be passed into every execute() method.
	 */
	public void execute(Operation operation, MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {

		if (operation == null) throw new NullPointerException();

		XRI3Segment target = operation.getTarget();

		// check if the target is a statement or an address

		Statement targetStatement = null;
		XRI3Segment targetAddress = null;

		try {

			targetStatement = AbstractStatement.fromXriSegment(target);
		} catch (Xdi2ParseException ex) {

			targetAddress = target;
		}

		// execute on address or statement

		if (targetStatement == null) {

			// execute target interceptors (address)

			if ((targetAddress = this.executeTargetInterceptorsAddress(operation, targetAddress, messageResult, executionContext)) == null) {

				return;
			}

			// execute contributors (address)

			if (this.executeContributorsAddress(targetAddress, operation, messageResult, executionContext)) {

				return;
			}

			// get an address handler, and execute on the address

			AddressHandler addressHandler = this.getAddressHandler(targetAddress);

			if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Executing " + operation.getOperationXri() + " on address " + targetAddress + " (" + addressHandler.getClass().getName() + ").");

			if (addressHandler.executeOnAddress(targetAddress, operation, messageResult, executionContext)) {

				return;
			}
		} else {

			// execute target interceptors (statement)

			if ((targetStatement = this.executeTargetInterceptorsStatement(operation, targetStatement, messageResult, executionContext)) == null) {

				return;
			}

			// execute contributors (statement)

			if (this.executeContributorsStatement(targetStatement, operation, messageResult, executionContext)) {

				return;
			}

			// get a statement handler, and execute on the statement

			StatementHandler statementHandler = this.getStatementHandler(targetStatement);

			if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Executing " + operation.getOperationXri() + " on statement " + targetStatement + " (" + statementHandler.getClass().getName() + ").");

			if (statementHandler.executeOnStatement(targetStatement, operation, messageResult, executionContext)) {

				return;
			}
		}
	}

	/*
	 * These are for being overridden by subclasses
	 */

	public void before(MessageEnvelope messageEnvelope, ExecutionContext executionContext) throws Xdi2MessagingException {

	}

	public void before(Message message, ExecutionContext executionContext) throws Xdi2MessagingException {

	}

	public void before(Operation operation, ExecutionContext executionContext) throws Xdi2MessagingException {

	}

	public void after(MessageEnvelope messageEnvelope, ExecutionContext executionContext) throws Xdi2MessagingException {

	}

	public void after(Message message, ExecutionContext executionContext) throws Xdi2MessagingException {

	}

	public void after(Operation operation, ExecutionContext executionContext) throws Xdi2MessagingException {

	}

	public void exception(MessageEnvelope messageEnvelope, ExecutionContext executionContext, Exception ex) throws Xdi2MessagingException {

	}

	public AddressHandler getAddressHandler(XRI3Segment targetAddress) throws Xdi2MessagingException {

		return null;
	}

	public StatementHandler getStatementHandler(Statement targetStatement) throws Xdi2MessagingException {

		return null;
	}

	/*
	 * Contributors
	 */

	private boolean executeContributorsAddress(XRI3Segment targetAddress, Operation operation, MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {

		XRI3Segment contextNodeXri = targetAddress;

		List<Contributor> contributors = this.getContributors().get(contextNodeXri);
		if (contributors == null) return false;

		for (Contributor contributor : contributors) {

			if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Executing contributor " + contributor.getClass().getSimpleName() + " (address).");

			if (contributor.executeOnAddress(targetAddress, operation, messageResult, executionContext)) {

				if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Address has been fully handled by contributor " + contributor.getClass().getSimpleName() + ".");
				return true;
			}
		}

		return false;
	}

	private boolean executeContributorsStatement(Statement targetStatement, Operation operation, MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {

		XRI3Segment contextNodeXri = targetStatement instanceof ContextNodeStatement ? new XRI3Segment(targetStatement.getSubject().toString() + targetStatement.getObject().toString()) : targetStatement.getSubject();

		List<Contributor> contributors = this.getContributors().get(contextNodeXri);
		if (contributors == null) return false;

		for (Contributor contributor : contributors) {

			if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Executing contributor " + contributor.getClass().getSimpleName() + " (statement).");

			if (contributor.executeOnStatement(targetStatement, operation, messageResult, executionContext)) {

				if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Address has been fully handled by contributor " + contributor.getClass().getSimpleName() + ".");
				return true;
			}
		}

		return false;
	}

	/*
	 * Interceptors
	 */

	private boolean executeMessageEnvelopeInterceptorsBefore(MessageEnvelope messageEnvelope, MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {

		for (Iterator<MessageEnvelopeInterceptor> messageEnvelopeInterceptors = this.getMessageEnvelopeInterceptors(); messageEnvelopeInterceptors.hasNext(); ) {

			MessageEnvelopeInterceptor messageEnvelopeInterceptor = messageEnvelopeInterceptors.next();

			if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Executing message envelope interceptor " + messageEnvelopeInterceptor.getClass().getSimpleName() + " (before).");

			if (messageEnvelopeInterceptor.before(messageEnvelope, messageResult, executionContext)) {

				if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Message envelope has been fully handled by interceptor " + messageEnvelopeInterceptor.getClass().getSimpleName() + ".");
				return true;
			}
		}

		return false;
	}

	private boolean executeMessageEnvelopeInterceptorsAfter(MessageEnvelope messageEnvelope, MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {

		for (Iterator<MessageEnvelopeInterceptor> messageEnvelopeInterceptors = this.getMessageEnvelopeInterceptors(); messageEnvelopeInterceptors.hasNext(); ) {

			MessageEnvelopeInterceptor messageEnvelopeInterceptor = messageEnvelopeInterceptors.next();

			if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Executing message envelope interceptor " + messageEnvelopeInterceptor.getClass().getSimpleName() + " (after).");

			if (messageEnvelopeInterceptor.after(messageEnvelope, messageResult, executionContext)) {

				if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Message envelope has been fully handled by interceptor " + messageEnvelopeInterceptor.getClass().getSimpleName() + ".");
				return true;
			}
		}

		return false;
	}

	private void executeMessageEnvelopeInterceptorsException(MessageEnvelope messageEnvelope, MessageResult messageResult, ExecutionContext executionContext, Exception ex) throws Xdi2MessagingException {

		for (Iterator<MessageEnvelopeInterceptor> messageEnvelopeInterceptors = this.getMessageEnvelopeInterceptors(); messageEnvelopeInterceptors.hasNext(); ) {

			MessageEnvelopeInterceptor messageEnvelopeInterceptor = messageEnvelopeInterceptors.next();

			if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Executing message envelope interceptor " + messageEnvelopeInterceptor.getClass().getSimpleName() + " (exception).");

			try {

				messageEnvelopeInterceptor.exception(messageEnvelope, messageResult, executionContext, ex);
			} catch (Exception ex2) {

				if (log.isWarnEnabled()) log.warn(this.getClass().getSimpleName() + ": Exception during message envelope interceptor " + messageEnvelopeInterceptor.getClass().getSimpleName() + " (exception): " + ex2.getMessage() + ".", ex2);
				continue;
			}
		}
	}

	private boolean executeMessageInterceptorsBefore(Message message, MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {

		for (Iterator<MessageInterceptor> messageInterceptors = this.getMessageInterceptors(); messageInterceptors.hasNext(); ) {

			MessageInterceptor messageInterceptor = messageInterceptors.next();

			if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Executing message interceptor " + messageInterceptor.getClass().getSimpleName() + " (before).");

			if (messageInterceptor.before(message, messageResult, executionContext)) {

				if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Message has been fully handled by interceptor " + messageInterceptor.getClass().getSimpleName() + ".");
				return true;
			}
		}

		return false;
	}

	private boolean executeMessageInterceptorsAfter(Message message, MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {

		for (Iterator<MessageInterceptor> messageInterceptors = this.getMessageInterceptors(); messageInterceptors.hasNext(); ) {

			MessageInterceptor messageInterceptor = messageInterceptors.next();

			if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Executing message interceptor " + messageInterceptor.getClass().getSimpleName() + " (after).");

			if (messageInterceptor.after(message, messageResult, executionContext)) {

				if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Message has been fully handled by interceptor " + messageInterceptor.getClass().getSimpleName() + ".");
				return true;
			}
		}

		return false;
	}

	private boolean executeOperationInterceptorsBefore(Operation operation, MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {

		for (Iterator<OperationInterceptor> operationInterceptors = this.getOperationInterceptors(); operationInterceptors.hasNext(); ) {

			OperationInterceptor operationInterceptor = operationInterceptors.next();

			if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Executing operation interceptor " + operationInterceptor.getClass().getSimpleName() + " (before).");

			if (operationInterceptor.before(operation, messageResult, executionContext)) {

				if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Operation has been fully handled by interceptor " + operationInterceptor.getClass().getSimpleName() + ".");
				return true;
			}
		}

		return false;
	}

	private boolean executeOperationInterceptorsAfter(Operation operation, MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {

		for (Iterator<OperationInterceptor> operationInterceptors = this.getOperationInterceptors(); operationInterceptors.hasNext(); ) {

			OperationInterceptor operationInterceptor = operationInterceptors.next();

			if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Executing operation interceptor " + operationInterceptor.getClass().getSimpleName() + " (after).");

			if (operationInterceptor.after(operation, messageResult, executionContext)) {

				if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Operation has been fully handled by interceptor " + operationInterceptor.getClass().getSimpleName() + ".");
				return true;
			}
		}

		return false;
	}

	private XRI3Segment executeTargetInterceptorsAddress(Operation operation, XRI3Segment targetAddress, MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {

		for (Iterator<TargetInterceptor> targetInterceptors = this.getTargetInterceptors(); targetInterceptors.hasNext(); ) {

			TargetInterceptor targetInterceptor = targetInterceptors.next();

			if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Executing target interceptor " + targetInterceptor.getClass().getSimpleName() + " on address " + targetAddress + ".");

			targetAddress = targetInterceptor.targetAddress(operation, targetAddress, messageResult, executionContext);

			if (targetAddress == null) {

				if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Address has been skipped by interceptor " + targetInterceptor.getClass().getSimpleName() + ".");
				return null;
			}

			if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Interceptor " + targetInterceptor.getClass().getSimpleName() + " returned address: " + targetAddress + ".");
		}

		return targetAddress;
	}

	private Statement executeTargetInterceptorsStatement(Operation operation, Statement targetStatement, MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {

		for (Iterator<TargetInterceptor> targetInterceptors = this.getTargetInterceptors(); targetInterceptors.hasNext(); ) {

			TargetInterceptor targetInterceptor = targetInterceptors.next();

			if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Executing target interceptor " + targetInterceptor.getClass().getSimpleName() + " on statement " + targetStatement + ".");

			targetStatement = targetInterceptor.targetStatement(operation, targetStatement, messageResult, executionContext);

			if (targetStatement == null) {

				if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Statement has been skipped by interceptor " + targetInterceptor.getClass().getSimpleName() + ".");
				return null;
			}

			if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Interceptor " + targetInterceptor.getClass().getSimpleName() + " returned statement: " + targetStatement + ".");
		}

		return targetStatement;
	}

	private void executeResultInterceptorsFinish(MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {

		for (Iterator<ResultInterceptor> resultInterceptors = this.getResultInterceptors(); resultInterceptors.hasNext(); ) {

			ResultInterceptor resultInterceptor = resultInterceptors.next();

			if (log.isDebugEnabled()) log.debug(this.getClass().getSimpleName() + ": Executing result interceptor " + resultInterceptor.getClass().getSimpleName() + " (finish).");

			resultInterceptor.finish(messageResult, executionContext);
		}
	}

	/*
	 * Getters and setters
	 */

	@Override
	public XRI3Segment getOwner() {

		return this.owner;
	}

	public void setOwner(XRI3Segment owner) {

		this.owner = owner;
	}

	public Map<XRI3Segment, List<Contributor>> getContributors() {

		return this.contributors;
	}

	public void setContributors(Map<XRI3Segment, List<Contributor>> contributors) {

		this.contributors = contributors;
	}

	public void addContributor(XRI3Segment xri, Contributor contributor) {

		List<Contributor> contributors = this.contributors.get(xri);

		if (contributors == null) {

			contributors = new ArrayList<Contributor> ();
			this.contributors.put(xri, contributors);
		}

		contributors.add(contributor);
	}

	public void removeContributor(XRI3Segment xri, Contributor contributor) {

		List<Contributor> contributors = this.contributors.get(xri);
		if (contributors == null) return;

		contributors.remove(contributor);

		if (contributors.isEmpty()) {

			this.contributors.remove(xri);
		}
	}

	public List<Interceptor> getInterceptors() {

		return this.interceptors;
	}

	public void setInterceptors(List<Interceptor> interceptors) {

		this.interceptors = interceptors;
	}

	public Iterator<MessagingTargetInterceptor> getMessagingTargetInterceptors() {

		return new SelectingClassIterator<Interceptor, MessagingTargetInterceptor> (this.interceptors.iterator(), MessagingTargetInterceptor.class);
	}

	public Iterator<MessageEnvelopeInterceptor> getMessageEnvelopeInterceptors() {

		return new SelectingClassIterator<Interceptor, MessageEnvelopeInterceptor> (this.interceptors.iterator(), MessageEnvelopeInterceptor.class);
	}

	public Iterator<MessageInterceptor> getMessageInterceptors() {

		return new SelectingClassIterator<Interceptor, MessageInterceptor> (this.interceptors.iterator(), MessageInterceptor.class);
	}

	public Iterator<OperationInterceptor> getOperationInterceptors() {

		return new SelectingClassIterator<Interceptor, OperationInterceptor> (this.interceptors.iterator(), OperationInterceptor.class);
	}

	public Iterator<TargetInterceptor> getTargetInterceptors() {

		return new SelectingClassIterator<Interceptor, TargetInterceptor> (this.interceptors.iterator(), TargetInterceptor.class);
	}

	public Iterator<ResultInterceptor> getResultInterceptors() {

		return new SelectingClassIterator<Interceptor, ResultInterceptor> (this.interceptors.iterator(), ResultInterceptor.class);
	}
}