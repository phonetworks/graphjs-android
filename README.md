# GraphJS-Android

GraphJS-Android is a Kotlin client-side library to help developers easily enable social features on their mobile apps. With only a few lines of code, you can easily add authentication, comments, messages, forum, groups, profiles etc. to your static web pages. 

## Getting Started

To get started with GraphJS, you need to include graphjs library as sources (via git-submodule for example) to your project. Then you can initiate it with ApiManager constructor.

Example:
```kotlin
<script>
ApiManager({{ANDROID_CONTEXT}}, {{GRAPHJS_HORT}}, {{PUBLIC_ID}})
</script>
```

## License

GNU Affero General Public License v3.0, see [LICENSE](https://github.com/phonetworks/graphjs/blob/master/LICENSE).
