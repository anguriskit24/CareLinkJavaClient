package info.nightscout.medtronic.carelink.client;

import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.ArrayList;


import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class PersistableOkHttpCookieJar implements CookieJar {

    private List<Cookie> storage = new ArrayList<>();

    private final String cookiePath;

    public PersistableOkHttpCookieJar(String cookiePath) {
        this.cookiePath = cookiePath;

        // load memory storage with existing cookies from file
        loadFromFile();
        removeExpiredCookies();
    }

    private void loadFromFile() {
        // Read existing cookies from file and add to memory storage

        File file = new File(cookiePath);

        // Create new file if it doesn't already exist
        try {
            file.createNewFile();

        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedSource source = Okio.buffer(Okio.source(file))) {
            String line;
            while ((line = source.readUtf8Line()) != null) {

                Cookie cookie = Cookie.parse(HttpUrl.parse("http://localhost"), line);

                // if cookie does not already exist in storage, add to storage
                if (cookie != null && !this.storage.contains(cookie.name())) {
                    storage.add(cookie);
                }
            }
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }

        // TODO: Clear file contents - should be a better way to do this
        try (BufferedSink sink = Okio.buffer(Okio.sink(file))) {
            sink.writeUtf8("");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {

        // Write cookies from memory storage to file
        try (BufferedSink sink = Okio.buffer(Okio.appendingSink(new File(cookiePath)))) {

            for (Cookie cookie : cookies) {

                // if cookie is new, add to memory storage
                if(!this.contains(cookie.name())) {

                    this.storage.add(cookie);
                    // If carelink cookie, save to file
                    if(cookie.name().equals(CareLinkClient.CARELINK_AUTH_TOKEN_COOKIE_NAME) ||
                            cookie.name().equals(CareLinkClient.CARELINK_TOKEN_VALIDTO_COOKIE_NAME)) {
                        sink.writeUtf8(cookie.toString());
                        sink.writeUtf8("\n");
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {

        List<Cookie> cookies = new ArrayList<>();

        // Remove expired Cookies
        removeExpiredCookies();

        // Only return matching Cookies
        for (int i = 0; i < storage.size(); i++) {
            if(storage.get(i).matches(url)) {
                cookies.add(storage.get(i));
            }
        }

        return cookies;
    }

    public List<Cookie> getCookies(String name) {

        List<Cookie> cookies = new ArrayList<>();

        // Remove expired Cookies
        removeExpiredCookies();

        // Only return matching Cookies
        for (int i = 0; i < storage.size(); i++) {
            if(storage.get(i).name().equals(name)) {
                cookies.add(storage.get(i));
            }
        }

        return cookies;

    }


    public boolean contains(String name) {
        return (getCookies(name).size() > 0);
    }

    public void deleteCookie(String name) {
        for (int i = 0; i < storage.size(); i++) {
            if(storage.get(i).name() == name) {
                storage.remove(i);
            }
        }
    }

    public void deleteAllCookies() {
        storage.clear();
    }

    private void removeExpiredCookies(){
        for (int i = 0; i < storage.size(); i++) {
            if(storage.get(i).expiresAt() < System.currentTimeMillis()) {
                storage.remove(i);
            }
        }
    }

}
