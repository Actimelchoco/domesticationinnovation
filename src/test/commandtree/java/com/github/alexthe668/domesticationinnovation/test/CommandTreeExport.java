package com.github.alexthe668.domesticationinnovation.test;
import com.mojang.brigadier.tree.*;
import com.mojang.brigadier.arguments.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.nio.file.*;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class CommandTreeExport {
    private static int nodes,commands;
    private static String arg(CommandNode<CommandSourceStack> node){
        if(!(node instanceof ArgumentCommandNode<?,?> argument))return node.getName();
        Object type=argument.getType();String kind;
        if(type instanceof StringArgumentType text)kind=switch(text.getType()) {
            case SINGLE_WORD -> "word";
            case QUOTABLE_PHRASE -> "string";
            case GREEDY_PHRASE -> "remaining text";
        };
        else if(type instanceof IntegerArgumentType||type instanceof FloatArgumentType||type instanceof DoubleArgumentType||type instanceof LongArgumentType||type instanceof BoolArgumentType)kind=type.toString();
        else kind=type.getClass().getSimpleName().replace("Argument","");
        return "<"+node.getName()+"> {"+kind+"}";
    }
    private static boolean allowed(CommandNode<CommandSourceStack> n,CommandSourceStack s){try{return n.canUse(s);}catch(Exception ignored){return false;}}
    private static String access(CommandNode<CommandSourceStack> n,CommandSourceStack player,CommandSourceStack console){
        boolean c=allowed(n,console);int min=-1;
        for(int level=0;level<=4;level++)if(allowed(n,player.withPermission(level))){min=level;break;}
        if(min==0&&c)return "";
        if(min<0&&!c)return " [conditional/hidden]";
        if(min<0)return " [console or conditional]";
        if(min==0)return " [player-only]";
        return " [op "+min+"+"+(c?"":", player-only")+"]";
    }
    private static void tree(StringBuilder out,CommandNode<CommandSourceStack> n,String prefix,boolean last,CommandSourceStack player,CommandSourceStack console){
        nodes++;if(n.getCommand()!=null)commands++;
        out.append(prefix).append(last?"`-- ":"|-- ").append(arg(n));
        if(n.getCommand()!=null)out.append(" *");
        out.append(access(n,player,console));
        if(n.getRedirect()!=null)out.append(" -> /").append(n.getRedirect().getName());
        out.append('\n');
        var children=new ArrayList<>(n.getChildren());
        children.sort(Comparator.comparing(CommandNode::getName,String.CASE_INSENSITIVE_ORDER));
        for(int i=0;i<children.size();i++)tree(out,children.get(i),prefix+(last?"    ":"|   "),i==children.size()-1,player,console);
    }
    @GameTest(template="empty") public static void exportRegisteredTamesCommandTree(GameTestHelper h)throws Exception{
        var server=h.getLevel().getServer();var dispatcher=server.getCommands().getDispatcher();
        var root=dispatcher.getRoot().getChild("tames");
        if(root==null)throw new AssertionError("/tames not registered");
        var fake=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"TreeExport"));
        var player=fake.createCommandSourceStack().withPermission(0);var console=server.createCommandSourceStack().withPermission(4);
        nodes=0;commands=0;StringBuilder out=new StringBuilder();
        out.append("TAMES COMMAND TREE - v75 - 2026-10-24\n")
           .append("Extracted from the actual registered Forge/Brigadier dispatcher.\n")
           .append("Alias: /tame redirects to /tames.\n\n")
           .append("HOW TO READ\n")
           .append("  Indentation shows the next word/argument in a command. Start with /tames.\n")
           .append("  * = executable at this point; children are optional continuations.\n")
           .append("  <name> = value to supply; {type} describes its argument parser/range.\n")
           .append("  word = one word; string = one word or quoted text; remaining text = rest of line.\n")
           .append("  Access labels apply to that branch and its descendants.\n")
           .append("  [op N+] = minimum permission observed for an idle player.\n")
           .append("  [player-only] = this node requires a player in the idle snapshot.\n")
           .append("  [conditional/hidden] = unavailable in this idle snapshot, including legacy hidden nodes.\n")
           .append("  Conditional commands can depend on duel phase, pending invites, confirmations, etc.\n")
           .append("  No label does not bypass parent requirements or checks performed when executing.\n")
           .append("  This is the complete registered tree, including hidden/conditional branches.\n")
           .append("  Selections and tab suggestions depend on your tames, groups and current state.\n")
           .append("  Type names normally use typeId; duplicate paths retain modId:typeId.\n\n");
        tree(out,root,"",true,player,console);
        out.append("\nTOTAL: ").append(nodes).append(" nodes, ").append(commands).append(" executable endpoints.\n");
        Files.writeString(Path.of("tames-command-tree-v75.txt"),out.toString());
        h.succeed();
    }
}
