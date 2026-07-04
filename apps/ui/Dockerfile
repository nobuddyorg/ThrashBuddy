FROM node:24@sha256:fdddfb3e688158251943d52eba361de991548f6814007acba4917ae6b512d6be AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm install
COPY . .
RUN npx ng build --configuration production

FROM nginx:alpine
COPY --from=build /app/dist/thrash-buddy/browser /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
