package fintechfrauds.serve.security;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
  private final byte[] cachedBody;

  CachedBodyHttpServletRequest(HttpServletRequest request, byte[] cachedBody) {
    super(request);
    this.cachedBody = cachedBody.clone();
  }

  @Override
  public ServletInputStream getInputStream() {
    ByteArrayInputStream inputStream = new ByteArrayInputStream(cachedBody);
    return new ServletInputStream() {
      @Override
      public boolean isFinished() {
        return inputStream.available() == 0;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(ReadListener readListener) {
        // no-op
      }

      @Override
      public int read() {
        return inputStream.read();
      }
    };
  }

  @Override
  public BufferedReader getReader() throws IOException {
    return new BufferedReader(new InputStreamReader(getInputStream()));
  }
}
