This example shows how to extend the `NavServer`.

It shows how to add features like push-buttons, and take ownership of a screen (Nokia, SSD1306...)

It comes with a class named `navserver.ServerWithButtons`, that extends the `navrest.NavServer`.
As a result, it's driven by the exact same `properties` file.

To see how to interact with the buttons (to start and stop the logging for example, or
to shutdown the whole server), look for the variables named `buttonOne` and `buttonTwo`.
```java
 private GpioPinDigitalInput buttonOne = null;
 private GpioPinDigitalInput buttonTwo = null;
```

This is built just like the other examples in this module, just run
```
 $ ./builder.sh
 ```
 and follow the instructions in the console.
 
 ---
 
 More to come. Diagrams, screenshots, pictures.
