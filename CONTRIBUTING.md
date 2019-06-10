# How to contribute

First of all thank you for contributing to SEPA, we really needs brave volunteers as you.🌟👍

If you haven't already, come find us on gitter ([#sepa_dev/Lobby#](irc://chat.freenode.net/opengovernment) on freenode). We want you working on things you're excited about.

Here are some important resources:

  * [Research Group](https://site.unibo.it/wot/en) tells you who we are and what we are doing
  * [Here](https://www.mdpi.com/1999-5903/10/4/36/htm) you can read a more detailed description of Sparql Event Processing archietecture if you want to contribute to the platform you are invated to read it,
  * [Protocol specification](http://mml.arces.unibo.it/TR/sparql11-subscribe.html) is our vision on how to deliver SPARQL query events
  * Bugs? [Github Issues](https://github.com/arces-wot/SEPA/issues) is where to report them
  * If you want contact us feel free to send an email to @lroffia and @relu91
  
## Testing

We have developed a set of unit tests for basic functionalities of the engine. Before submit your contribution verify that they are 
succesful in your development enviroment. Use the following command:
```bash
maven test
```
Moreover the complex interactions with the underlaying SPARQL endpoint are tested with a set of integration tests. To run them first
you need to have a running blazegraph instance in your localmachine and then use this following command inside the main folder of the project repository:
```bash
maven verify
```

## Submitting changes

Please send a [GitHub Pull Request to arces-wot/SEPA](https://github.com/arces-wot/SEPA/pull/new/dev) following the template and with a clear list of what you've done (read more about [pull requests](http://help.github.com/pull-requests/)). 
When you send a pull request, we will love you forever if you also add tests that check your changes. 
We can always use more test coverage. 
Please make sure all commits are atomic (one feature per commit).

Always write a clear log message for your commits. One-line messages are fine for small changes, but bigger changes should look like this:

    $ git commit -m "A brief summary of the commit
    > 
    > A paragraph describing what changed and its impact."

Futhermore, use the following commit convention:
 * Start the commit with *Add* if you added a feature
 * Start with *Modify* if you changed some behaviour of previous features
 * Start with *Fix* if your commit contain a fix of a bug
 * Follow the good practise [guide](https://github.com/RomuloOliveira/commit-messages-guide) for commit
    
We use as the base branch the dev, branch from there and add your changes. PR against master branch will be rejected, master branch is only used for releases.

Thanks,

Luca and Cristiano 🙌
