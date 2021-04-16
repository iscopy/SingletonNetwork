package com.xolo.singletonnetwork;

import android.annotation.SuppressLint;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.Connection;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpHeaders;
import okhttp3.internal.platform.Platform;
import okio.Buffer;
import okio.BufferedSource;

import static okhttp3.internal.platform.Platform.INFO;

public class HttpLogging implements Interceptor {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    public enum Level {
        /**
         * No logs.
         */
        NONE,
        /**
         * Logs request and response lines.
         *
         * <p>Example:
         * <pre>{@code
         * --> POST /greeting http/1.1 (3-byte body)
         *
         * <-- 200 OK (22ms, 6-byte body)
         * }</pre>
         */
        BASIC,
        /**
         * Logs request and response lines and their respective headers.
         *
         * <p>Example:
         * <pre>{@code
         * --> POST /greeting http/1.1
         * Host: example.com
         * Content-Type: plain/text
         * Content-Length: 3
         * --> END POST
         *
         * <-- 200 OK (22ms)
         * Content-Type: plain/text
         * Content-Length: 6
         * <-- END HTTP
         * }</pre>
         */
        HEADERS,
        /**
         * Logs request and response lines and their respective headers and bodies (if present).
         *
         * <p>Example:
         * <pre>{@code
         * --> POST /greeting http/1.1
         * Host: example.com
         * Content-Type: plain/text
         * Content-Length: 3
         *
         * Hi?
         * --> END POST
         *
         * <-- 200 OK (22ms)
         * Content-Type: plain/text
         * Content-Length: 6
         *
         * Hello!
         * <-- END HTTP
         * }</pre>
         */
        BODY
    }

    public interface Logger {
        void log(String message);

        /**
         * A {@link HttpLogging.Logger} defaults output appropriate for the current platform.
         */
        HttpLogging.Logger DEFAULT = new HttpLogging.Logger() {
            @Override
            public void log(String message) {
                Platform.get().log(INFO, message, null);
            }
        };
    }

    public HttpLogging() {
        this(HttpLogging.Logger.DEFAULT);
    }

    public HttpLogging(HttpLogging.Logger logger) {
        this.logger = logger;
    }

    private final HttpLogging.Logger logger;

    private volatile HttpLogging.Level level = HttpLogging.Level.NONE;

    /**
     * Change the level at which this interceptor logs.
     */
    public HttpLogging setLevel(HttpLogging.Level level) {
        if (level == null) throw new NullPointerException("level == null. Use Level.NONE instead.");
        this.level = level;
        return this;
    }

    public HttpLogging.Level getLevel() {
        return level;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        StringBuffer stringBuffer = new StringBuffer();

        //是否打印其他log
        boolean logs = true;

        //设置一个随机数，用于区分多个请求的返回
        String head = String.valueOf(new Random(1000));

        HttpLogging.Level level = this.level;

        Request request = chain.request();
        if (level == HttpLogging.Level.NONE) {
            return chain.proceed(request);
        }

        boolean logBody = level == HttpLogging.Level.BODY;
        boolean logHeaders = logBody || level == HttpLogging.Level.HEADERS;

        RequestBody requestBody = request.body();
        boolean hasRequestBody = requestBody != null;

        Connection connection = chain.connection();
        Protocol protocol = connection != null ? connection.protocol() : Protocol.HTTP_1_1;
        String requestStartMessage = "--> " + request.method() + ' ' + request.url() + ' ' + protocol;
        if (!logHeaders && hasRequestBody) {
            requestStartMessage += " (" + requestBody.contentLength() + "-byte body)";
        }
        if (logs)
            logger.log(head + "    " + requestStartMessage);
        //头部信息
        String headerStr = "";
        //参数信息
        String parameter = "";
        if (logHeaders) {
            if (hasRequestBody) {
                // Request body headers are only present when installed as a network interceptor. Force
                // them to be included (when available) so there values are known.
                if (requestBody.contentType() != null) {
                    if (logs)
                        logger.log(head + "    " + "Content-Type: " + requestBody.contentType());
                }
                if (requestBody.contentLength() != -1) {
                    if (logs)
                        logger.log(head + "    " + "Content-Length: " + requestBody.contentLength());
                }
            }

            Headers headers = request.headers();
            for (int i = 0, count = headers.size(); i < count; i++) {
                String name = headers.name(i);
                // Skip headers from the request body as they are explicitly logged above.
                if (!"Content-Type" .equalsIgnoreCase(name) && !"Content-Length" .equalsIgnoreCase(name)) {
                    logger.log(head + "    " + name + ": " + headers.value(i));
                }
            }
            //头部信息
            headerStr = headers.toString();
            if (!logBody || !hasRequestBody) {
                if (logs)
                    logger.log(head + "    " + "--> END " + request.method());
            } else if (bodyEncoded(request.headers())) {
                if (logs)
                    logger.log(head + "    " + "--> END " + request.method() + " (encoded body omitted)");
            } else {
                Buffer buffer = new Buffer();
                requestBody.writeTo(buffer);

                Charset charset = UTF8;
                MediaType contentType = requestBody.contentType();
                if (contentType != null) {
                    charset = contentType.charset(UTF8);
                }

                //logger.log(head + "    " + "");
                if (isPlaintext(buffer)) {
                    //参数信息
                    parameter = buffer.readString(charset);
                    logger.log(head + "    " + parameter);
                    if (logs)
                        logger.log(head + "    " + "--> END " + request.method() + " (" + requestBody.contentLength() + "-byte body)");
                } else {
                    if (logs)
                        logger.log(head + "    " + "--> END " + request.method() + " (binary " + requestBody.contentLength() + "-byte body omitted)");
                }
            }
        }

        long startNs = System.nanoTime();
        Response response;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            if (logs)
                logger.log(head + "    " + "<-- HTTP FAILED: " + e);
            throw e;
        }
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

        ResponseBody responseBody = response.body();
        long contentLength = responseBody.contentLength();
        String bodySize = contentLength != -1 ? contentLength + "-byte" : "unknown-length";
        logger.log(head + "    " + "<-- " + response.code() + ' ' + response.message() + ' '
                + response.request().url() + " (" + tookMs + "ms" + (!logHeaders ? ", "
                + bodySize + " body" : "") + ')');
        String resultsUrl = "";
        if (response.code() != 200) {
            logger.log(response.code() + " " + response.header("Location"));
            resultsUrl = response.code() + ' ' + response.message() + ' ' + response.request().url();
        }

        if (logHeaders) {
            Headers headers = response.headers();
            for (int i = 0, count = headers.size(); i < count; i++) {
                if (logs)
                    logger.log(head + "    " + headers.name(i) + ": " + headers.value(i));
            }

            if (!logBody || !HttpHeaders.hasBody(response)) {
                if (logs)
                    logger.log(head + "    " + "<-- END HTTP");
            } else if (bodyEncoded(response.headers())) {
                if (logs)
                    logger.log(head + "    " + "<-- END HTTP (encoded body omitted)");
            } else {
                BufferedSource source = responseBody.source();
                source.request(Long.MAX_VALUE); // Buffer the entire body.
                Buffer buffer = source.buffer();

                Charset charset = UTF8;
                MediaType contentType = responseBody.contentType();
                if (contentType != null) {
                    try {
                        charset = contentType.charset(UTF8);
                    } catch (UnsupportedCharsetException e) {
                        if (logs) {
                            logger.log(head + "    " + "");
                            logger.log(head + "    " + "Couldn't decode the response body; charset is likely malformed.");
                            logger.log(head + "    " + "<-- END HTTP");
                        }

                        return response;
                    }
                }

                if (!isPlaintext(buffer)) {
                    //logger.log(head + "    " + "");
                    if (logs)
                        logger.log(head + "    " + "<-- END HTTP (binary " + buffer.size() + "-byte body omitted)");
                    return response;
                }

                if (contentLength != 0) {
                    //logger.log(head + "    " + "");
                    logger.log(head + "    " + buffer.clone().readString(charset));
                }
                if (logs)
                    logger.log(head + "    " + "<-- END HTTP (" + buffer.size() + "-byte body)");
            }
        }
        return response;
    }

    /**
     * Returns true if the body in question probably contains human readable text. Uses a small sample
     * of code points to detect unicode control characters commonly used in binary file signatures.
     */
    static boolean isPlaintext(Buffer buffer) {
        try {
            Buffer prefix = new Buffer();
            long byteCount = buffer.size() < 64 ? buffer.size() : 64;
            buffer.copyTo(prefix, 0, byteCount);
            for (int i = 0; i < 16; i++) {
                if (prefix.exhausted()) {
                    break;
                }
                int codePoint = prefix.readUtf8CodePoint();
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false;
                }
            }
            return true;
        } catch (EOFException e) {
            return false; // Truncated UTF-8 sequence.
        }
    }

    private boolean bodyEncoded(Headers headers) {
        String contentEncoding = headers.get("Content-Encoding");
        return contentEncoding != null && !contentEncoding.equalsIgnoreCase("identity");
    }

}
