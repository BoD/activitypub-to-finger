# activitypub-to-finger

Did you know Microblogging existed before Twitter? In fact the concept even predates blogs - and the web!

The [Finger protocol](https://en.wikipedia.org/wiki/Finger_(protocol)) from **1977** (the first `finger` implementations is from **1971**!)
is a simple way to share information about users.

In a terminal, type `finger <username>@<host>` and you should get a short description of the user and what they're up to.

This project is a server that bridges the [ActivityPub](https://en.wikipedia.org/wiki/ActivityPub) protocol (mostly known for its use
in [Mastodon](https://en.wikipedia.org/wiki/Mastodon_(social_network))) to the Finger protocol.

So now you can do `finger <mastodon username>@<host running this server>` and get the user's latest Mastodon posts.

For instance, try `finger @BoD@mastodon.social@finger.JRAF.org`, and you should see my latest posts.

## Docker instructions

### Building and pushing the image

```sh
./gradlew dockerBuild
docker push bodlulu/activitypub-to-finger
```

### Running the image

```sh
docker pull bodlulu/activitypub-to-finger
docker run \
  --publish 79:7900 \
  --publish <HTTP PORT TO LISTEN TO>:8042 \
  --env PUBLIC_HTTP_SERVER_NAME="finger.example.com" \
  --env DEFAULT_ADDRESS="@someUser@example.com" \
  --env DEFAULT_ADDRESS_ALIAS="someUser" \
  bodlulu/activitypub-to-finger
```

## License

```
Copyright (C) 2025-present Benoit 'BoD' Lubek (BoD@JRAF.org)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
```
