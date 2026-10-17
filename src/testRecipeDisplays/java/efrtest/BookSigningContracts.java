package efrtest;

import ganymedes01.etfuturum.mixins.early.bookinput.client.MixinGuiScreenBook;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.lang.reflect.*;

/** Executes the injected handlers without opening a window; actual client rendering remains a small smoke test. */
public final class BookSigningContracts {
    private static final class Book extends MixinGuiScreenBook {
        int updates;
        @Override protected void updateButtons() { updates++; }
    }
    private static Field field(String name) throws Exception { Field f=MixinGuiScreenBook.class.getDeclaredField(name);f.setAccessible(true);return f; }
    private static Method method(String name,Class<?>... args) throws Exception { Method m=MixinGuiScreenBook.class.getDeclaredMethod(name,args);m.setAccessible(true);return m; }
    private static void check(boolean ok,String why) { if(!ok)throw new AssertionError(why); }
    public static void run() throws Exception {
        Book book=new Book();field("field_146480_s").setBoolean(book,true);field("bookIsUnsigned").setBoolean(book,true);field("bookTitle").set(book,"");field("etfuturum$committedInput").set(book,true);
        Method key=method("etfuturum$titleKey",char.class,int.class,CallbackInfo.class),text=method("etfuturum$committedTitle",String.class,CallbackInfo.class);
        for(char c:"Letter test".toCharArray()) {
            CallbackInfo ordinary=new CallbackInfo("title",true);key.invoke(book,c,30,ordinary);check(ordinary.isCancelled(),"committed-text hook also admitted ordinary printable key");
            CallbackInfo committed=new CallbackInfo("text",true);text.invoke(book,String.valueOf(c),committed);check(committed.isCancelled(),"signing text fell through into page contents");
        }
        check(field("bookTitle").get(book).equals("Letter test"),"duplicated/missing signing letters");
        for(int k:new int[]{14,28,156}) { CallbackInfo control=new CallbackInfo("title",true);key.invoke(book,'\0',k,control);check(!control.isCancelled(),"backspace/enter lost"); }
        CallbackInfo pasted=new CallbackInfo("text",true);text.invoke(book," too long\u00a7ignored",pasted);check(((String)field("bookTitle").get(book)).length()==16,"title limit bypass");
        field("field_146480_s").setBoolean(book,false);CallbackInfo page=new CallbackInfo("text",true);text.invoke(book,"page contents",page);check(!page.isCancelled(),"normal page editor intercepted");
        field("etfuturum$committedInput").set(book,false);CallbackInfo legacy=new CallbackInfo("title",true);key.invoke(book,'x',45,legacy);check(!legacy.isCancelled(),"legacy LWJGL printable key suppressed");
        check(field("field_146481_r").getBoolean(book) && book.updates>0,"title edits not marked/synced to buttons");
        System.out.println("BOOK_SIGNING PASSED: one title append per committed event, page isolation, control keys, legacy path and title length; no window opened");
    }
}
