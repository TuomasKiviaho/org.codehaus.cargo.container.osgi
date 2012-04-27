package org.apache.maven.surefire.osgi;

import java.beans.Introspector;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.maven.surefire.booter.BooterDeserializer;
import org.apache.maven.surefire.booter.ForkingRunListener;
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.report.ConsoleLogger;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferIndexFinder;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.DefaultChannelPipeline;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.oio.OioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;

@Aspect()
public class ForkedBooterAspect {

	public static final String PORT = "org.apache.maven.surefire.osgi.port";
	public static final String HOSTNAME = "org.apache.maven.surefire.osgi.hostname";

	private static Charset CHARSET = Charset.forName("UTF-8");

	private static class TelnetEncoder extends StringEncoder {

		private static class ChannelBufferIndexFinderImpl implements
				ChannelBufferIndexFinder {

			public ChannelBufferIndexFinderImpl() {
				super();
			}

			public boolean find(ChannelBuffer channelBuffer, int index) {
				byte character = channelBuffer.getByte(index);
				return character < 32 || character == '^' || character == 255;
			}

		}

		private ChannelBufferIndexFinder channelBufferIndexFinder;

		public TelnetEncoder(Charset charset) {
			super(charset);
			this.channelBufferIndexFinder = new ChannelBufferIndexFinderImpl();
		}

		@Override
		protected Object encode(ChannelHandlerContext channelHandlerContext,
				Channel channel, Object message) throws Exception {
			Object encodedMessage = message;
			if (message instanceof String) {
				ChannelBuffer channelBuffer = (ChannelBuffer) super.encode(
						channelHandlerContext, channel, message);
				for (int readableBytes = channelBuffer.readableBytes(), index = channelBuffer
						.indexOf(0, readableBytes,
								this.channelBufferIndexFinder); !(index < 0); index = channelBuffer
						.indexOf(index, ++readableBytes,
								this.channelBufferIndexFinder)) {
					ChannelBuffer prefix = channelBuffer.copy(0, index);
					byte character = channelBuffer.getByte(index);
					byte[] bytes = character == 255 ? new byte[] { character,
							character } : new byte[] { '^',
							(byte) (character + '@') };
					index += bytes.length;
					ChannelBuffer postfix = index < readableBytes ? channelBuffer
							.copy(index, readableBytes)
							: ChannelBuffers.EMPTY_BUFFER;
					channelBuffer = ChannelBuffers.wrappedBuffer(prefix,
							ChannelBuffers.wrappedBuffer(bytes), postfix);
				}
				encodedMessage = channelBuffer;
			}
			return encodedMessage;
		}

	}

	private static class TelnetDecoder extends StringDecoder {

		private static class ChannelBufferIndexFinderImpl implements
				ChannelBufferIndexFinder {

			public ChannelBufferIndexFinderImpl() {
				super();
			}

			public boolean find(ChannelBuffer channelBuffer, int index) {
				byte character = channelBuffer.getByte(index);
				return character == '^' || character == 255;
			}

		}

		private ChannelBufferIndexFinder channelBufferIndexFinder;

		public TelnetDecoder(Charset charset) {
			super(charset);
			this.channelBufferIndexFinder = new ChannelBufferIndexFinderImpl();
		}

		@Override
		protected Object decode(ChannelHandlerContext channelHandlerContext,
				Channel channel, Object message) throws Exception {
			Object decodedMessage = message;
			if (message instanceof ChannelBuffer) {
				ChannelBuffer channelBuffer = (ChannelBuffer) message;
				for (int readableBytes = channelBuffer.readableBytes(), index = channelBuffer
						.indexOf(0, readableBytes, channelBufferIndexFinder); !(index < 0); index = channelBuffer
						.indexOf(index, --readableBytes,
								channelBufferIndexFinder)) {
					ChannelBuffer prefix = channelBuffer.copy(0, index);
					if (index < readableBytes) {
						byte character = channelBuffer
								.getByte(index + 1);
						if (character != 255) {
							character -= '@';
						}
						byte[] bytes = new byte[] { character };
						ChannelBuffer postfix = index + 1 < readableBytes ? channelBuffer
								.copy(index + 1, readableBytes)
								: ChannelBuffers.EMPTY_BUFFER;
						channelBuffer = ChannelBuffers.wrappedBuffer(prefix,
								ChannelBuffers.wrappedBuffer(bytes), postfix);
					} else {
						channelBuffer = prefix;
					}
				}
				decodedMessage = super.decode(channelHandlerContext, channel,
						channelBuffer);
			}
			return decodedMessage;
		}
	}

	private static class TelnetHandler extends SimpleChannelHandler {

		private static final String LINE_SEPARATOR = System
				.getProperty("line.separator");
		private static final String BYE = "Z,0,";

		private ConsoleLogger consoleLogger;
		private PrintStream printStream;
		private volatile String message;

		public TelnetHandler(PrintStream printStream) {
			this.printStream = printStream;
			this.consoleLogger = new ForkingRunListener(System.out, 0);
		}

		@Override
		public void writeRequested(ChannelHandlerContext channelHandlerContext,
				MessageEvent messageEvent) throws Exception {
			ChannelBuffer message = (ChannelBuffer) messageEvent.getMessage();
			this.message = message.toString(CHARSET);
			ChannelBuffer lineDelimiter = Delimiters.lineDelimiter()[1];
			message = ChannelBuffers.wrappedBuffer(message, lineDelimiter);
			Channel channel = messageEvent.getChannel();
			ChannelFuture channelFuture = messageEvent.getFuture();
			SocketAddress remoteAddress = messageEvent.getRemoteAddress();
			MessageEvent downstreamMessageEvent = new DownstreamMessageEvent(
					channel, channelFuture, message, remoteAddress);
			super.writeRequested(channelHandlerContext, downstreamMessageEvent);
		}

		@Override
		public void exceptionCaught(
				ChannelHandlerContext channelHandlerContext,
				ExceptionEvent exceptionEvent) throws Exception {
			Throwable cause = exceptionEvent.getCause();
			cause.printStackTrace(this.printStream);
		}

		@Override
		public void messageReceived(
				ChannelHandlerContext channelHandlerContext,
				MessageEvent messageEvent) throws Exception {
			String message = (String) messageEvent.getMessage();
			if (this.consoleLogger == null) {
				this.printStream.println(message);
				if (message.startsWith(BYE)) {
					Channel channel = messageEvent.getChannel();
					byte[] bytes = new byte[] { 4 };
					channel.write(ChannelBuffers.wrappedBuffer(bytes));
				}
			} else {
				this.consoleLogger.info(message + LINE_SEPARATOR);
				if (this.message != null && message.endsWith(this.message)) {
					this.consoleLogger = null;
				}
			}
		}

	};

	@Around("execution(public static void org.apache.maven.surefire.booter.ForkedBooter.main(String[])) && args(args)")
	public void main(ProceedingJoinPoint proceedingJoinPoint, String[] args)
			throws Throwable {
		File surefirePropertiesFile = new File(args[0]);
		InputStream inputStream = surefirePropertiesFile.exists() ? new FileInputStream(
				surefirePropertiesFile) : null;
		BooterDeserializer booterDeserializer = new BooterDeserializer(
				inputStream);
		ProviderConfiguration providerConfiguration = booterDeserializer
				.deserialize();
		Properties providerProperties = providerConfiguration
				.getProviderProperties();
		String hostname = providerProperties.getProperty(HOSTNAME, "127.0.0.1");
		String property = providerProperties.getProperty(PORT, "6666");
		int port = Integer.parseInt(property);
		SocketAddress socketAddress = hostname == null ? new InetSocketAddress(
				port) : new InetSocketAddress(hostname, port);
		ReporterConfiguration reporterConfiguration = providerConfiguration
				.getReporterConfiguration();
		PrintStream originalSystemOut = reporterConfiguration
				.getOriginalSystemOut();
		Executor executor = Executors.newCachedThreadPool();
		ChannelFactory channelFactory = new OioClientSocketChannelFactory(
				executor);
		ClientBootstrap clientBootstrap = new ClientBootstrap(channelFactory);
		try {
			ChannelDownstreamHandler stringEncoder = new TelnetEncoder(CHARSET);
			ChannelUpstreamHandler stringDecoder = new TelnetDecoder(CHARSET);
			ChannelUpstreamHandler delimiterBasedFrameDecoder = new DelimiterBasedFrameDecoder(
					8192, Delimiters.lineDelimiter());
			ChannelHandler telnetHandler = new TelnetHandler(originalSystemOut);
			ChannelPipeline channelPipeLine = new DefaultChannelPipeline();
			for (ChannelHandler channelHandler : Arrays.asList(
					delimiterBasedFrameDecoder, stringDecoder, telnetHandler,
					stringEncoder)) {
				Class<? extends ChannelHandler> chanelHandlerClass = channelHandler
						.getClass();
				String name = Introspector.decapitalize(chanelHandlerClass
						.getSimpleName());
				channelPipeLine.addLast(name, channelHandler);
			}
			clientBootstrap.setPipeline(channelPipeLine);
			ChannelFuture channelFuture = clientBootstrap
					.connect(socketAddress);
			Channel channel = channelFuture.getChannel();
			{
				StringBuilder stringBuilder = new StringBuilder();
				String scope = Booter.class.getPackage().getName();
				stringBuilder.append(scope);
				stringBuilder.append(':');
				String function = Booter.class.getSimpleName();
				stringBuilder.append(function);
				for (String arg : args) {
					stringBuilder.append(' ');
					String[] tokens = arg.split("\\'");
					for (int i = 0; i < tokens.length; i++) {
						if (i > 0) {
							stringBuilder.append("\\'");
						}
						stringBuilder.append('\'');
						stringBuilder.append(tokens[i]);
						stringBuilder.append('\'');
					}
				}
				String message = stringBuilder.toString();
				channel.write(message);
			}
			ChannelFuture closeFuture = channel.getCloseFuture();
			closeFuture.await();
		} finally {
			clientBootstrap.releaseExternalResources();
		}
	}

}
