package ganymedes01.etfuturum.core.utils;

import com.google.gson.*;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
import java.util.*;

/** Read-only plain/JSON book pages, with inherited formatting and explicitly described links. */
public final class LecternBookText {
    private LecternBookText() {}
    public static final class Span {
        public final String text,format,action,value,hover;
        private Span(String text,String format,String action,String value,String hover) {
            this.text=text;this.format=format;this.action=action;this.value=value;this.hover=hover;
        }
    }
    public static List<Span> parse(String text,boolean signed) {
        if(text==null)text="";
        if(!signed || !(text.startsWith("{") || text.startsWith("[") || text.startsWith("\"")))return Collections.singletonList(new Span(text,"","","",""));
        try {
            List<Span> spans=new ArrayList<>();read(new JsonParser().parse(text),new ChatStyle(),"","","",spans,0);
            return Collections.unmodifiableList(spans);
        }catch(RuntimeException | StackOverflowError invalid) { return Collections.singletonList(new Span(text,"","","","")); }
    }
    private static void read(JsonElement node,ChatStyle parent,String action,String value,String hover,List<Span> spans,int depth) {
        if(depth>64)throw new IllegalArgumentException("Deep book component");
        if(node==null || node.isJsonNull())return;
        if(node.isJsonPrimitive()) { spans.add(new Span(node.getAsString(),format(parent),action,value,hover));return; }
        if(node.isJsonArray()) {
            JsonArray array=node.getAsJsonArray();if(array.size()==0)return;
            JsonElement first=array.get(0);
            if(first.isJsonObject() || first.isJsonPrimitive()) {
                JsonObject root;
                if(first.isJsonObject())root=new JsonParser().parse(first.toString()).getAsJsonObject();
                else { root=new JsonObject();root.addProperty("text",first.getAsString()); }
                JsonArray extra=root.has("extra")?root.getAsJsonArray("extra"):new JsonArray();
                for(int i=1;i<array.size();i++)extra.add(array.get(i));root.add("extra",extra);
                read(root,parent,action,value,hover,spans,depth+1);
            } else for(JsonElement part:array)read(part,parent,action,value,hover,spans,depth+1);
            return;
        }
        JsonObject object=node.getAsJsonObject();
        JsonObject own=new JsonParser().parse(object.toString()).getAsJsonObject();
        own.remove("extra");own.remove("clickEvent");own.remove("click_event");own.remove("hoverEvent");own.remove("hover_event");
        IChatComponent component=IChatComponent.Serializer.func_150699_a(own.toString());if(component==null)return;
        ChatStyle style=component.getChatStyle().setParentStyle(parent);
        JsonElement click=object.has("click_event")?object.get("click_event"):object.get("clickEvent");
        if(click!=null && click.isJsonObject()) {
            JsonObject event=click.getAsJsonObject();action=string(event,"action","");
            String key="change_page".equals(action)?"page":"open_url".equals(action)?"url":("run_command".equals(action) || "suggest_command".equals(action))?"command":"value";
            value=string(event,key,string(event,"value",""));
        }
        JsonElement hovered=object.has("hover_event")?object.get("hover_event"):object.get("hoverEvent");
        if(hovered!=null && hovered.isJsonObject()) {
            JsonObject event=hovered.getAsJsonObject();
            if("show_text".equals(string(event,"action",""))) {
                JsonElement content=event.has("contents")?event.get("contents"):event.get("value");
                if(content!=null) {
                    List<Span> detail=new ArrayList<>();read(content,new ChatStyle(),"","","",detail,depth+1);
                    StringBuilder label=new StringBuilder();for(Span part:detail)label.append(part.format).append(part.text);hover=label.toString();
                }
            }
        }
        spans.add(new Span(component.getUnformattedTextForChat(),format(style),action,value,hover));
        if(object.has("extra"))for(JsonElement extra:object.getAsJsonArray("extra"))read(extra,style,action,value,hover,spans,depth+1);
    }
    private static String format(ChatStyle style) {
        // ChatStyle.getFormattingCode is stripped on dedicated servers in 1.7.10.
        StringBuilder result=new StringBuilder();
        if(style.getColor()!=null)result.append(style.getColor());
        if(style.getBold())result.append(net.minecraft.util.EnumChatFormatting.BOLD);
        if(style.getItalic())result.append(net.minecraft.util.EnumChatFormatting.ITALIC);
        if(style.getUnderlined())result.append(net.minecraft.util.EnumChatFormatting.UNDERLINE);
        if(style.getObfuscated())result.append(net.minecraft.util.EnumChatFormatting.OBFUSCATED);
        if(style.getStrikethrough())result.append(net.minecraft.util.EnumChatFormatting.STRIKETHROUGH);
        return result.toString();
    }
    private static String string(JsonObject object,String key,String fallback) {
        JsonElement value=object.get(key);return value!=null && value.isJsonPrimitive()?value.getAsString():fallback;
    }
}
