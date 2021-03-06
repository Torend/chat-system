package controllers;

import actors.Action;
import actors.Op;
import akka.actor.ActorRef;
import play.data.DynamicForm;
import play.data.Form;
import play.mvc.*;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 * TODO: understand how play framework works to make a frontend with it.
 */
public class HomeController extends Controller {

    private final ActorRef lookupActor;

    @Inject
    public HomeController(@Named("lookup-actor") ActorRef lookupActor) {
        this.lookupActor = lookupActor;
    }

    /**
     * An action that renders an HTML page with a welcome message.
     * The configuration in the <code>routes</code> file means that
     * this method will be called when the application receives a
     * <code>GET</code> request with a path of <code>/</code>.
     */
    public Result index() {
        /*
        this is the homepage, when you enter localhost:9000 the actions here will happen.
        currently I used it to test messaging.
         */
        lookupActor.tell(new Action.Connect("fucker"), null);
        lookupActor.tell(new Action.SendText("fucker", "fuck"), null);
        return ok(views.html.index.render());
    }

    public Result add(int a, int b) {
        // Fire-and-forget
        //lookupActor.tell(new Action.Connect("fucker"), null);
        //lookupActor.tell(new Action.SendText("fucker", "fuck"), null);
        lookupActor.tell(new Op.Add(a, b), null);
        return ok("Add: See the logs for result");
    }

    public Result subtract(int a, int b) {
        // Fire-and-forget
        lookupActor.tell(new Op.Subtract(a, b), null);
        return ok("Subtract: See the logs for result");
    }

    public Result sendTo()
    {
        //DynamicForm form = Form.form().bindFromRequest();
        return ok("fuc kyo");

    }
}
